/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.common;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlValidationError;
import org.roda.core.RodaCoreFactory;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.ip.File;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.metadata.FileFormat;
import org.roda.core.data.v2.ip.metadata.Fixity;
import org.roda.core.data.v2.ip.metadata.IndexedPreservationAgent;
import org.roda.core.data.v2.ip.metadata.PreservationMetadata.PreservationMetadataType;
import org.roda.core.data.v2.validation.ValidationException;
import org.roda.core.data.v2.validation.ValidationIssue;
import org.roda.core.data.v2.validation.ValidationReport;
import org.roda.core.metadata.MetadataException;
import org.roda.core.metadata.MetadataHelperUtility;
import org.roda.core.model.ModelService;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.plugins.Plugin;
import org.roda.core.storage.Binary;
import org.roda.core.storage.ContentPayload;
import org.roda.core.storage.StringContentPayload;
import org.roda.core.storage.fs.FSUtils;
import org.roda.core.util.FileUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.util.DateParser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import lc.xmlns.premisV2.AgentComplexType;
import lc.xmlns.premisV2.AgentDocument;
import lc.xmlns.premisV2.AgentIdentifierComplexType;
import lc.xmlns.premisV2.ContentLocationComplexType;
import lc.xmlns.premisV2.CreatingApplicationComplexType;
import lc.xmlns.premisV2.EventComplexType;
import lc.xmlns.premisV2.EventDocument;
import lc.xmlns.premisV2.EventIdentifierComplexType;
import lc.xmlns.premisV2.EventOutcomeDetailComplexType;
import lc.xmlns.premisV2.EventOutcomeInformationComplexType;
import lc.xmlns.premisV2.FixityComplexType;
import lc.xmlns.premisV2.FormatComplexType;
import lc.xmlns.premisV2.FormatDesignationComplexType;
import lc.xmlns.premisV2.FormatRegistryComplexType;
import lc.xmlns.premisV2.LinkingAgentIdentifierComplexType;
import lc.xmlns.premisV2.LinkingObjectIdentifierComplexType;
import lc.xmlns.premisV2.ObjectCharacteristicsComplexType;
import lc.xmlns.premisV2.ObjectComplexType;
import lc.xmlns.premisV2.ObjectDocument;
import lc.xmlns.premisV2.ObjectIdentifierComplexType;
import lc.xmlns.premisV2.RelatedObjectIdentificationComplexType;
import lc.xmlns.premisV2.RelationshipComplexType;
import lc.xmlns.premisV2.Representation;
import lc.xmlns.premisV2.StorageComplexType;

public class PremisUtils {
  private static final String SEPARATOR ="_";
  private final static Logger LOGGER = LoggerFactory.getLogger(PremisUtils.class);
  private static final String W3C_XML_SCHEMA_NS_URI = "http://www.w3.org/2001/XMLSchema";

  public static Fixity calculateFixity(Binary binary, String digestAlgorithm, String originator)
    throws IOException, NoSuchAlgorithmException {
    InputStream dsInputStream = binary.getContent().createInputStream();
    Fixity fixity = new Fixity(digestAlgorithm, FileUtility.calculateChecksumInHex(dsInputStream, digestAlgorithm),
      originator);
    dsInputStream.close();
    return fixity;
  }

  public static Binary addFormatToPremis(Binary preservationFile, FileFormat format)
    throws IOException, XmlException, MetadataException, RequestNotValidException, NotFoundException, GenericException {
    lc.xmlns.premisV2.File f = lc.xmlns.premisV2.File.Factory.parse(preservationFile.getContent().createInputStream());
    if (f.getObjectCharacteristicsList() != null && f.getObjectCharacteristicsList().size() > 0) {
      ObjectCharacteristicsComplexType occt = f.getObjectCharacteristicsList().get(0);
      if (occt.getFormatList() != null && occt.getFormatList().size() > 0) {
        FormatComplexType fct = occt.getFormatList().get(0);
        if (fct.getFormatDesignation() != null) {
          fct.getFormatDesignation().setFormatName(format.getMimeType());
        } else {
          fct.addNewFormatDesignation().setFormatName(format.getMimeType());
        }
      } else {
        FormatComplexType fct = occt.addNewFormat();
        fct.addNewFormatDesignation().setFormatName(format.getMimeType());
      }
    } else {
      ObjectCharacteristicsComplexType occt = f.addNewObjectCharacteristics();
      FormatComplexType fct = occt.addNewFormat();
      fct.addNewFormatDesignation().setFormatName(format.getMimeType());
    }

    Path temp = Files.createTempFile("file", ".premis.xml");

    org.roda.core.metadata.MetadataHelperUtility.saveToFile(f, temp.toFile());
    return (Binary) FSUtils.convertPathToResource(temp.getParent(), temp);
  }

  public static boolean isPremisV2(Binary binary, Path configBasePath) throws IOException, SAXException {
    boolean premisV2 = true;
    InputStream inputStream = binary.getContent().createInputStream();
    InputStream schemaStream = RodaCoreFactory.getConfigurationFileAsStream("schemas/premis-v2-0.xsd");
    Source xmlFile = new StreamSource(inputStream);
    SchemaFactory schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
    Schema schema = schemaFactory.newSchema(new StreamSource(schemaStream));
    Validator validator = schema.newValidator();
    RodaErrorHandler errorHandler = new RodaErrorHandler();
    validator.setErrorHandler(errorHandler);
    try {
      validator.validate(xmlFile);
      List<SAXParseException> errors = errorHandler.getErrors();
      if (errors.size() > 0) {
        premisV2 = false;
      }
    } catch (SAXException e) {
      premisV2 = false;
    }
    return premisV2;
  }

  public static Binary updatePremisToV3IfNeeded(Binary binary, Path configBasePath) throws IOException, SAXException,
    TransformerException, RequestNotValidException, NotFoundException, GenericException {
    if (isPremisV2(binary, configBasePath)) {
      LOGGER.debug("Binary " + binary.getStoragePath().asString() + " is Premis V2... Needs updated...");
      return updatePremisV2toV3(binary, configBasePath);
    } else {
      return binary;
    }

  }

  private static Binary updatePremisV2toV3(Binary binary, Path configBasePath)
    throws IOException, TransformerException, RequestNotValidException, NotFoundException, GenericException {
    InputStream transformerStream = null;
    InputStream bais = null;

    try {
      Map<String, Object> stylesheetOpt = new HashMap<String, Object>();
      Reader reader = new InputStreamReader(binary.getContent().createInputStream());
      transformerStream = RodaCoreFactory.getConfigurationFileAsStream("crosswalks/migration/v2Tov3.xslt");
      Reader xsltReader = new InputStreamReader(transformerStream);
      CharArrayWriter transformerResult = new CharArrayWriter();
      RodaUtils.applyStylesheet(xsltReader, reader, stylesheetOpt, transformerResult);
      Path p = Files.createTempFile("preservation", ".tmp");
      bais = new ByteArrayInputStream(transformerResult.toString().getBytes("UTF-8"));
      Files.copy(bais, p, StandardCopyOption.REPLACE_EXISTING);

      return (Binary) FSUtils.convertPathToResource(p.getParent(), p);
    } catch (IOException e) {
      throw e;
    } catch (TransformerException e) {
      throw e;
    } finally {
      if (transformerStream != null) {
        try {
          transformerStream.close();
        } catch (IOException e) {

        }
      }
      if (bais != null) {
        try {
          bais.close();
        } catch (IOException e) {

        }
      }
    }
  }

  private static class RodaErrorHandler extends DefaultHandler {
    List<SAXParseException> errors;

    public RodaErrorHandler() {
      errors = new ArrayList<SAXParseException>();
    }

    public void warning(SAXParseException e) throws SAXException {
      errors.add(e);
    }

    public void error(SAXParseException e) throws SAXException {
      errors.add(e);
    }

    public void fatalError(SAXParseException e) throws SAXException {
      errors.add(e);
    }

    public List<SAXParseException> getErrors() {
      return errors;
    }

    public void setErrors(List<SAXParseException> errors) {
      this.errors = errors;
    }

  }

  public static Binary updateFile(Binary preservationFile, IndexedFile file)
    throws XmlException, IOException, GenericException {
    ObjectDocument document = ObjectDocument.Factory.newInstance();
    FileFormat fileFormat = file.getFileFormat();
    lc.xmlns.premisV2.File f = binaryToFile(preservationFile.getContent().createInputStream());
    if (fileFormat != null) {

      if (!StringUtils.isBlank(fileFormat.getFormatDesignationName())) {
        FormatDesignationComplexType fdct = getFormatDesignation(f);
        fdct.setFormatName(fileFormat.getFormatDesignationName());
      }
      if (!StringUtils.isBlank(fileFormat.getFormatDesignationVersion())) {
        FormatDesignationComplexType fdct = getFormatDesignation(f);
        fdct.setFormatVersion(fileFormat.getFormatDesignationVersion());
      }
      if (!StringUtils.isBlank(fileFormat.getMimeType())) {
        FormatDesignationComplexType fdct = getFormatDesignation(f);
        fdct.setFormatName(fileFormat.getFormatDesignationName());
      }

      if (!StringUtils.isBlank(fileFormat.getPronom())) {
        FormatRegistryComplexType frct = getFormatRegistry(f, RodaConstants.PRESERVATION_REGISTRY_PRONOM);
        frct.setFormatRegistryKey(fileFormat.getPronom());
      }

      if (!StringUtils.isBlank(file.getCreatingApplicationName())) {
        CreatingApplicationComplexType cact = getCreatingApplication(f);
        cact.setCreatingApplicationName(file.getCreatingApplicationName());
      }

      if (!StringUtils.isBlank(file.getCreatingApplicationVersion())) {
        CreatingApplicationComplexType cact = getCreatingApplication(f);
        cact.setCreatingApplicationVersion(file.getCreatingApplicationVersion());
      }

      if (!StringUtils.isBlank(file.getDateCreatedByApplication())) {
        CreatingApplicationComplexType cact = getCreatingApplication(f);
        cact.setDateCreatedByApplication(file.getDateCreatedByApplication());
      }
    }

    document.setObject(f);
    try {
      Path eventPath = Files.createTempFile("file", ".premis.xml");
      MetadataHelperUtility.saveToFile(document, eventPath.toFile());
      return (Binary) FSUtils.convertPathToResource(eventPath.getParent(), eventPath);
    } catch (IOException | MetadataException | GenericException | RequestNotValidException | NotFoundException e) {

    }
    return null;
  }

  private static CreatingApplicationComplexType getCreatingApplication(lc.xmlns.premisV2.File f) {
    ObjectCharacteristicsComplexType occt;
    CreatingApplicationComplexType cact;
    if (f.getObjectCharacteristicsList() != null && f.getObjectCharacteristicsList().size() > 0) {
      occt = f.getObjectCharacteristicsList().get(0);
    } else {
      occt = f.addNewObjectCharacteristics();
    }
    if (occt.getCreatingApplicationList() != null && occt.getCreatingApplicationList().size() > 0) {
      cact = occt.getCreatingApplicationArray(0);
    } else {
      cact = occt.addNewCreatingApplication();
    }
    return cact;
  }

  private static FormatRegistryComplexType getFormatRegistry(lc.xmlns.premisV2.File f, String registryName) {
    ObjectCharacteristicsComplexType occt;
    FormatComplexType fct;
    if (f.getObjectCharacteristicsList() != null && f.getObjectCharacteristicsList().size() > 0) {
      occt = f.getObjectCharacteristicsList().get(0);
    } else {
      occt = f.addNewObjectCharacteristics();
    }
    if (occt.getFormatList() != null && occt.getFormatList().size() > 0) {
      fct = occt.getFormatList().get(0);
    } else {
      fct = occt.addNewFormat();
    }
    FormatRegistryComplexType frct = fct.getFormatRegistry();
    if (frct == null) {
      frct = fct.addNewFormatRegistry();
      frct.setFormatRegistryName(registryName);
    }
    return frct;
  }

  private static FormatDesignationComplexType getFormatDesignation(lc.xmlns.premisV2.File f) {
    ObjectCharacteristicsComplexType occt;
    FormatComplexType fct;
    FormatDesignationComplexType fdct;
    if (f.getObjectCharacteristicsList() != null && f.getObjectCharacteristicsList().size() > 0) {
      occt = f.getObjectCharacteristicsList().get(0);
    } else {
      occt = f.addNewObjectCharacteristics();
    }
    if (occt.getFormatList() != null && occt.getFormatList().size() > 0) {
      fct = occt.getFormatList().get(0);
    } else {
      fct = occt.addNewFormat();
    }
    if (fct.getFormatDesignation() != null) {
      fdct = fct.getFormatDesignation();
    } else {
      fdct = fct.addNewFormatDesignation();
    }
    return fdct;
  }

  public static ContentPayload createPremisEventBinary(String eventID, Date date, String type, String details,
    List<String> sources, List<String> targets, String outcome, String detailNote, String detailExtension,
    List<IndexedPreservationAgent> agents) throws GenericException {
    EventDocument event = EventDocument.Factory.newInstance();
    EventComplexType ect = event.addNewEvent();
    EventIdentifierComplexType eict = ect.addNewEventIdentifier();
    eict.setEventIdentifierValue(eventID);
    eict.setEventIdentifierType("local");
    ect.setEventDateTime(DateParser.getIsoDate(date));
    ect.setEventType(type);
    ect.setEventDetail(details);
    if (sources != null) {
      for (String source : sources) {
        LinkingObjectIdentifierComplexType loict = ect.addNewLinkingObjectIdentifier();
        loict.setLinkingObjectIdentifierValue(source);
        loict.setLinkingObjectIdentifierType("source");
      }
    }

    if (targets != null) {
      for (String target : targets) {
        LinkingObjectIdentifierComplexType loict = ect.addNewLinkingObjectIdentifier();
        loict.setLinkingObjectIdentifierValue(target);
        loict.setLinkingObjectIdentifierType("target");
      }
    }

    if (agents != null) {
      for (IndexedPreservationAgent agent : agents) {
        LinkingAgentIdentifierComplexType agentIdentifier = ect.addNewLinkingAgentIdentifier();
        agentIdentifier.setLinkingAgentIdentifierType("local");
        agentIdentifier.setLinkingAgentIdentifierValue(agent.getId());
        agentIdentifier.setRole(agent.getRole());
        agentIdentifier.setTitle(agent.getTitle());
        agentIdentifier.setType("simple");
      }
    }
    EventOutcomeInformationComplexType outcomeInformation = ect.addNewEventOutcomeInformation();
    outcomeInformation.setEventOutcome(outcome);
    EventOutcomeDetailComplexType eodct = outcomeInformation.addNewEventOutcomeDetail();
    eodct.setEventOutcomeDetailNote(detailNote);

    // TODO handle...
    /*
     * if(detailExtension!=null){ ExtensionComplexType extension =
     * eodct.addNewEventOutcomeDetailExtension();
     * extension.set(XmlObject.Factory.newValue("<p>"+detailExtension+"</p>"));
     * }
     */
    try {
      return new StringContentPayload(MetadataHelperUtility.saveToString(event, true));
    } catch (MetadataException e) {
      throw new GenericException("Error creating Premis Event", e);
    }
  }

  public static ContentPayload createPremisAgentBinary(String id, String name, String type) throws GenericException {
    AgentDocument agent = AgentDocument.Factory.newInstance();

    AgentComplexType act = agent.addNewAgent();
    AgentIdentifierComplexType agentIdentifier = act.addNewAgentIdentifier();
    agentIdentifier.setAgentIdentifierType("local");
    agentIdentifier.setAgentIdentifierValue(id);
    act.addAgentName(name);
    act.setAgentType(type);
    try {
      return new StringContentPayload(MetadataHelperUtility.saveToString(agent, true));
    } catch (MetadataException e) {
      throw new GenericException("Error creating PREMIS agent binary", e);
    }
  }

  public static ContentPayload createBaseRepresentation(String aipID, String representationId) throws GenericException {
    ObjectDocument document = ObjectDocument.Factory.newInstance();
    Representation representation = Representation.Factory.newInstance();
    ObjectIdentifierComplexType oict = representation.addNewObjectIdentifier();
    oict.setObjectIdentifierType("local");
    String identifier = createPremisRepresentationIdentifier(aipID, representationId);
    oict.setObjectIdentifierValue(identifier);
    representation.addNewPreservationLevel().setPreservationLevelValue("");
    document.setObject(representation);
    try {
      return new StringContentPayload(MetadataHelperUtility.saveToString(document, true));
    } catch (MetadataException e) {
      throw new GenericException("Error creating base representation", e);
    }
  }

  public static ContentPayload createBaseFile(File originalFile, ModelService model)
    throws GenericException, RequestNotValidException, NotFoundException, AuthorizationDeniedException {
    ObjectDocument document = ObjectDocument.Factory.newInstance();
    lc.xmlns.premisV2.File file = lc.xmlns.premisV2.File.Factory.newInstance();
    file.addNewPreservationLevel().setPreservationLevelValue(RodaConstants.PRESERVATION_LEVEL_FULL);
    ObjectIdentifierComplexType oict = file.addNewObjectIdentifier();
    String identifier = createPremisFileIdentifier(originalFile);
    oict.setObjectIdentifierValue(identifier);
    oict.setObjectIdentifierType("local");
    ObjectCharacteristicsComplexType occt = file.addNewObjectCharacteristics();
    occt.setCompositionLevel(BigInteger.valueOf(0));
    FormatComplexType fct = occt.addNewFormat();
    FormatDesignationComplexType fdct = fct.addNewFormatDesignation();
    fdct.setFormatName("");
    fdct.setFormatVersion("");
    Binary binary = model.getStorage().getBinary(ModelUtils.getFileStoragePath(originalFile));
    try {
      Fixity md5 = calculateFixity(binary, "MD5", "");
      FixityComplexType fixityMD5 = occt.addNewFixity();
      fixityMD5.setMessageDigest(md5.getMessageDigest());
      fixityMD5.setMessageDigestAlgorithm(md5.getMessageDigestAlgorithm());
      fixityMD5.setMessageDigestOriginator(md5.getMessageDigestOriginator());
    } catch (IOException | NoSuchAlgorithmException e) {
      LOGGER.warn("Could not calculate fixity for file " + originalFile);
    }

    occt.setSize(binary.getSizeInBytes());
    // occt.addNewObjectCharacteristicsExtension().set("");
    file.addNewOriginalName().setStringValue(originalFile.getId());
    StorageComplexType sct = file.addNewStorage();
    ContentLocationComplexType clct = sct.addNewContentLocation();
    clct.setContentLocationType("");
    clct.setContentLocationValue("");

    document.setObject(file);
    try {
      return new StringContentPayload(MetadataHelperUtility.saveToString(document, true));
    } catch (MetadataException e) {
      throw new GenericException("Error creating base file", e);
    }
  }

  public static List<Fixity> extractFixities(Binary premisFile) throws GenericException, XmlException, IOException {
    List<Fixity> fixities = new ArrayList<Fixity>();
    lc.xmlns.premisV2.File f = binaryToFile(premisFile.getContent().createInputStream());
    if (f.getObjectCharacteristicsList() != null && f.getObjectCharacteristicsList().size() > 0) {
      ObjectCharacteristicsComplexType occt = f.getObjectCharacteristicsList().get(0);
      if (occt.getFixityList() != null && occt.getFixityList().size() > 0) {
        for (FixityComplexType fct : occt.getFixityList()) {
          Fixity fix = new Fixity();
          fix.setMessageDigest(fct.getMessageDigest());
          fix.setMessageDigestAlgorithm(fct.getMessageDigestAlgorithm());
          fix.setMessageDigestOriginator(fct.getMessageDigestOriginator());
          fixities.add(fix);
        }
      }
    }
    return fixities;
  }

  public static lc.xmlns.premisV2.Representation binaryToRepresentation(InputStream binaryInputStream)
    throws XmlException, IOException, GenericException {
    ObjectDocument objectDocument = ObjectDocument.Factory.parse(binaryInputStream);

    ObjectComplexType object = objectDocument.getObject();
    if (object instanceof Representation) {
      return (Representation) object;
    } else {
      throw new GenericException("Trying to load a representation but was a " + object.getClass().getSimpleName());
    }
  }

  public static lc.xmlns.premisV2.File binaryToFile(InputStream binaryInputStream)
    throws XmlException, IOException, GenericException {
    ObjectDocument objectDocument = ObjectDocument.Factory.parse(binaryInputStream);

    ObjectComplexType object = objectDocument.getObject();
    if (object instanceof lc.xmlns.premisV2.File) {
      return (lc.xmlns.premisV2.File) object;
    } else {
      throw new GenericException("Trying to load a file but was a " + object.getClass().getSimpleName());
    }
  }

  public static EventComplexType binaryToEvent(InputStream binaryInputStream) throws XmlException, IOException {
    return EventDocument.Factory.parse(binaryInputStream).getEvent();
  }

  public static AgentComplexType binaryToAgent(InputStream binaryInputStream) throws XmlException, IOException {
    return AgentDocument.Factory.parse(binaryInputStream).getAgent();
  }

  public static lc.xmlns.premisV2.Representation binaryToRepresentation(ContentPayload payload, boolean validate)
    throws ValidationException, GenericException {
    Representation representation;
    try {
      representation = binaryToRepresentation(payload.createInputStream());

      List<XmlValidationError> validationErrors = new ArrayList<>();
      XmlOptions validationOptions = new XmlOptions();
      validationOptions.setErrorListener(validationErrors);

      if (validate && !representation.validate(validationOptions)) {
        throw new ValidationException(xmlValidationErrorsToValidationReport(validationErrors));
      }
    } catch (XmlException | IOException e) {
      throw new GenericException("Error loading representation premis file", e);
    }

    return representation;
  }

  public static lc.xmlns.premisV2.File binaryToFile(ContentPayload payload, boolean validate)
    throws ValidationException, GenericException {
    lc.xmlns.premisV2.File file;
    try {
      file = binaryToFile(payload.createInputStream());

      List<XmlValidationError> validationErrors = new ArrayList<>();
      XmlOptions validationOptions = new XmlOptions();
      validationOptions.setErrorListener(validationErrors);

      if (validate && !file.validate(validationOptions)) {
        throw new ValidationException(xmlValidationErrorsToValidationReport(validationErrors));
      }
    } catch (XmlException | IOException e) {
      throw new GenericException("Error loading representation premis file", e);
    }

    return file;
  }

  public static EventComplexType binaryToEvent(ContentPayload payload, boolean validate)
    throws ValidationException, GenericException {
    EventComplexType event;
    try {
      event = binaryToEvent(payload.createInputStream());

      List<XmlValidationError> validationErrors = new ArrayList<>();
      XmlOptions validationOptions = new XmlOptions();
      validationOptions.setErrorListener(validationErrors);

      if (validate && !event.validate(validationOptions)) {
        throw new ValidationException(xmlValidationErrorsToValidationReport(validationErrors));
      }
    } catch (XmlException | IOException e) {
      throw new GenericException("Error loading representation premis file", e);
    }

    return event;
  }

  public static AgentComplexType binaryToAgent(ContentPayload payload, boolean validate)
    throws ValidationException, GenericException {
    AgentComplexType agent;
    try {
      agent = binaryToAgent(payload.createInputStream());

      List<XmlValidationError> validationErrors = new ArrayList<>();
      XmlOptions validationOptions = new XmlOptions();
      validationOptions.setErrorListener(validationErrors);

      if (validate && !agent.validate(validationOptions)) {
        throw new ValidationException(xmlValidationErrorsToValidationReport(validationErrors));
      }
    } catch (XmlException | IOException e) {
      throw new GenericException("Error loading representation premis file", e);
    }

    return agent;
  }

  private static ValidationReport xmlValidationErrorsToValidationReport(List<XmlValidationError> validationErrors) {
    ValidationReport report = new ValidationReport();
    report.setValid(false);
    List<ValidationIssue> issues = new ArrayList<>();
    for (XmlValidationError error : validationErrors) {
      ValidationIssue issue = new ValidationIssue();
      issue.setMessage(error.getMessage());
      issue.setColumnNumber(error.getColumn());
      issue.setLineNumber(error.getLine());
      issues.add(issue);
    }
    report.setIssues(issues);
    return report;
  }

  public static SolrInputDocument updateSolrDocument(SolrInputDocument doc, Binary premisBinary)
    throws GenericException {
    try {
      lc.xmlns.premisV2.File premisFile = binaryToFile(premisBinary.getContent().createInputStream());
      if (premisFile.getOriginalName() != null) {
        doc.setField(RodaConstants.FILE_ORIGINALNAME, premisFile.getOriginalName().getStringValue());

        // TODO extension
      }
      if (premisFile.getObjectCharacteristicsList() != null && premisFile.getObjectCharacteristicsList().size() > 0) {
        ObjectCharacteristicsComplexType occt = premisFile.getObjectCharacteristicsList().get(0);
        doc.setField(RodaConstants.FILE_SIZE, occt.getSize());
        if (occt.getFixityList() != null && occt.getFixityList().size() > 0) {
          List<String> hashes = new ArrayList<>();
          for (FixityComplexType fct : occt.getFixityList()) {
            StringBuilder fixityPrint = new StringBuilder();
            fixityPrint.append(fct.getMessageDigest());
            fixityPrint.append(" (");
            fixityPrint.append(fct.getMessageDigestAlgorithm());
            if (StringUtils.isNotBlank(fct.getMessageDigestOriginator())) {
              fixityPrint.append(", "); //
              fixityPrint.append(fct.getMessageDigestOriginator());
            }
            fixityPrint.append(")");
            hashes.add(fixityPrint.toString());
          }
          doc.addField(RodaConstants.FILE_HASH, hashes);
        }
        if (occt.getFormatList() != null && occt.getFormatList().size() > 0) {
          FormatComplexType fct = occt.getFormatList().get(0);
          if (fct.getFormatDesignation() != null) {
            doc.addField(RodaConstants.FILE_FILEFORMAT, fct.getFormatDesignation().getFormatName());
            doc.addField(RodaConstants.FILE_FORMAT_VERSION, fct.getFormatDesignation().getFormatVersion());
            doc.addField(RodaConstants.FILE_FORMAT_MIMETYPE, fct.getFormatDesignation().getFormatName());
          }
          if (fct.getFormatRegistry() != null && fct.getFormatRegistry().getFormatRegistryName()
            .equalsIgnoreCase(RodaConstants.PRESERVATION_REGISTRY_PRONOM)) {
            doc.addField(RodaConstants.FILE_PRONOM, fct.getFormatRegistry().getFormatRegistryKey());
          }
          // TODO extension
        }
        if (occt.getCreatingApplicationList() != null && occt.getCreatingApplicationList().size() > 0) {
          CreatingApplicationComplexType cact = occt.getCreatingApplicationList().get(0);
          doc.addField(RodaConstants.FILE_CREATING_APPLICATION_NAME, cact.getCreatingApplicationName());
          doc.addField(RodaConstants.FILE_CREATING_APPLICATION_VERSION, cact.getCreatingApplicationVersion());
          doc.addField(RodaConstants.FILE_DATE_CREATED_BY_APPLICATION, cact.getDateCreatedByApplication());
        }
      }

    } catch (XmlException | IOException e) {

    }
    return doc;
  }

  public static IndexedPreservationAgent createPremisAgentBinary(Plugin<?> plugin,
    String preservationAgentTypeCharacterizationPlugin, ModelService model) throws GenericException, NotFoundException,
      RequestNotValidException, AuthorizationDeniedException, ValidationException, AlreadyExistsException {
    String id = plugin.getClass().getName() + "@" + plugin.getVersion();
    ContentPayload agentPayload;
    agentPayload = PremisUtils.createPremisAgentBinary(id, plugin.getName(),
      RodaConstants.PRESERVATION_AGENT_TYPE_CHARACTERIZATION_PLUGIN);
    model.createPreservationMetadata(PreservationMetadataType.AGENT, id, agentPayload);
    IndexedPreservationAgent agent = getPreservationAgent(plugin,preservationAgentTypeCharacterizationPlugin,model);
    return agent;
  }
  
  public static IndexedPreservationAgent getPreservationAgent(Plugin<?> plugin,
    String preservationAgentTypeCharacterizationPlugin, ModelService model) {
    String id = plugin.getClass().getName() + "@" + plugin.getVersion();
    IndexedPreservationAgent agent = new IndexedPreservationAgent();
    agent.setId(id);
    agent.setIdentifierType("local");
    agent.setIdentifierValue(id);
    agent.setTitle(plugin.getName());
    return agent;
  }

  public static ContentPayload linkFileToRepresentation(File file, String aipId, String representationId,
    String relationshipType, String relationshipSubType, ModelService model) throws GenericException,
      RequestNotValidException, NotFoundException, AuthorizationDeniedException, XmlException, IOException {
    Binary preservationRepresentation = model.getStorage()
      .getBinary(ModelUtils.getPreservationRepresentationPath(aipId, representationId));
    Representation r = binaryToRepresentation(preservationRepresentation.getContent().createInputStream());
    RelationshipComplexType relationship = r.addNewRelationship();
    relationship.setRelationshipType(relationshipType);
    relationship.setRelationshipSubType(relationshipSubType);
    RelatedObjectIdentificationComplexType roict = relationship.addNewRelatedObjectIdentification();
    roict.setRelatedObjectIdentifierType(RodaConstants.PREMIS_IDENTIFIER_TYPE_LOCAL);
    roict.setRelatedObjectIdentifierValue(createPremisFileIdentifier(file));

    ObjectDocument document = ObjectDocument.Factory.newInstance();
    document.setObject(r);
    try {
      return new StringContentPayload(MetadataHelperUtility.saveToString(document, true));
    } catch (MetadataException e) {
      throw new GenericException("Error creating base representation", e);
    }
  }
  
  
  public static String createPremisRepresentationIdentifier(String aipId, String representationId) {
    return aipId+SEPARATOR+representationId;
  }
  public static String createPremisFileIdentifier(File f) {
    String identifier = createPremisRepresentationIdentifier(f.getAipId(), f.getRepresentationId());
    if(f.getPath()!=null && f.getPath().size()>0){
      identifier+=SEPARATOR;
      identifier+=StringUtils.join(f.getPath(),SEPARATOR);
    }
    identifier+=SEPARATOR;
    identifier+=f.getId();
    return identifier;
  }
}
