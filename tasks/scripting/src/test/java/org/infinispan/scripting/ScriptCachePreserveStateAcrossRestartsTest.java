package org.infinispan.scripting;

import static org.infinispan.commons.test.CommonsTestingUtil.loadFileAsString;
import static org.infinispan.commons.internal.InternalCacheNames.SCRIPT_CACHE_NAME;
import static org.testng.AssertJUnit.assertTrue;

import java.io.InputStream;

import org.infinispan.Cache;
import org.infinispan.commons.test.CommonsTestingUtil;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.AbstractInfinispanTest;
import org.infinispan.test.CacheManagerCallable;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.Test;


@Test(groups = "functional", testName = "scripting.ScriptCachePreserveStateAcrossRestarts")
public class ScriptCachePreserveStateAcrossRestartsTest extends AbstractInfinispanTest {

   protected EmbeddedCacheManager createCacheManager(String persistentStateLocation) throws Exception {
      GlobalConfigurationBuilder global = new GlobalConfigurationBuilder();
      global.globalState().enable().persistentLocation(persistentStateLocation);
      EmbeddedCacheManager cacheManager = TestCacheManagerFactory.createCacheManager(global, new ConfigurationBuilder());
      cacheManager.getCache();
      return cacheManager;
   }

   public void testStatePreserved() throws Exception {
      String persistentStateLocation = CommonsTestingUtil.tmpDirectory(this.getClass());
      Util.recursiveFileRemove(persistentStateLocation);

      TestingUtil.withCacheManager(new CacheManagerCallable(createCacheManager(persistentStateLocation)) {
         @Override
         public void call() throws Exception {
            Cache<String, String> scriptCache = cm.getCache(SCRIPT_CACHE_NAME);
            try (InputStream is = this.getClass().getResourceAsStream("/test.js")) {
               String script = loadFileAsString(is);
               scriptCache.put("test.js", script);
            }
         }
      });

      TestingUtil.withCacheManager(new CacheManagerCallable(createCacheManager(persistentStateLocation)) {
         @Override
         public void call() throws Exception {
            Cache<String, String> scriptCache = cm.getCache(SCRIPT_CACHE_NAME);
            assertTrue(scriptCache.containsKey("test.js"));
         }
      });
   }
}
