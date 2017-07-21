package com.devontrain.jaxb.plugins;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 20.07.17.
 */
public class MixInXJCPluginTest {

    @Test
    public void shortening() throws Exception {
        Set<String> names = new HashSet<>();
        String result;
        assertEquals("cn", MixInXJCPlugin.shortening(names, "CountryName", false));
        assertEquals("con", MixInXJCPlugin.shortening(names, "CompanyName", false));
        assertEquals("conn", MixInXJCPlugin.shortening(names, "ContactName", false));
        assertEquals("ct", MixInXJCPlugin.shortening(names, "ContactTitle", false));
        assertEquals("contn", MixInXJCPlugin.shortening(names, "ContractorName", false));
    }

}