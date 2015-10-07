package config.i18n.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import org.apache.commons.io.IOUtils;

public class Messages {
  private static final String MESSAGES_BUNDLE = "Messages";
  private ResourceBundle resourceBundle;

  public Messages(Locale locale, Path folder) {
    this.resourceBundle = ResourceBundle.getBundle(MESSAGES_BUNDLE, locale, new FolderBasedUTF8Control(folder));
  }

  /**
   * Get translation
   * 
   * @param key
   * @return
   */
  public String getTranslation(String key) {
    return resourceBundle.getString(key);
  }

  /**
   * 
   * prefix will be replaced by "i18n." for simplicity purposes
   */
  public <T> Map<String, T> getTranslations(String prefix, Class<T> valueClass, boolean replacePrefixFromKey) {
    Map<String, T> map = new HashMap<String, T>();
    Enumeration<String> keys = resourceBundle.getKeys();
    String fullPrefix = prefix + ".";
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      if (key.startsWith(fullPrefix)) {
        map.put(replacePrefixFromKey ? key.replaceFirst(fullPrefix, "i18n.") : key,
          valueClass.cast(resourceBundle.getString(key)));
      }
    }
    return map;
  }

  private class FolderBasedUTF8Control extends Control {
    private Path folder;

    public FolderBasedUTF8Control(Path folder) {
      this.folder = folder;
    }

    @Override
    public long getTimeToLive(String baseName, Locale locale) {
      // ask not to cache
      return TTL_DONT_CACHE;
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
      throws IllegalAccessException, InstantiationException, IOException {

      if (!"java.properties".equals(format)) {
        return null;
      }

      String bundleName = toBundleName(baseName, locale) + ".properties";
      ResourceBundle bundle = null;

      InputStreamReader reader = null;
      InputStream is = null;
      try {
        File file = folder.resolve(bundleName).toFile();

        // Also checks for file existence
        if (Files.exists(folder.resolve(bundleName))) {
          is = new FileInputStream(file);
        } else {
          is = this.getClass().getResourceAsStream(bundleName);
        }
        reader = new InputStreamReader(is, Charset.forName("UTF-8"));
        bundle = new PropertyResourceBundle(reader);
      } finally {
        IOUtils.closeQuietly(reader);
        IOUtils.closeQuietly(is);
      }
      return bundle;
    }
  }
}