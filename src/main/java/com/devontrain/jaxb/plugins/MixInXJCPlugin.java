package com.devontrain.jaxb.plugins;

import com.devontrain.jaxb.common.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.Module.SetupContext;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.devontrain.jaxb.common.CodeModelUtil.unwrap;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 23.06.17.
 */
@XJCPlugin(name = "Xshortins")
public class MixInXJCPlugin extends XJCPluginBase {

    private static final String CURRENT_PACKAGE = "internal";

    @XJCPluginProperty(defaultValue = CURRENT_PACKAGE)
    private String pckg = CURRENT_PACKAGE;

    @XJCPluginCustomizations(uri = "http://common.jaxb.devontrain.com/plugin/shortins-generator")
    private interface shortins {
        enum cust {
            generate
        }
    }

    @Override
    public boolean run(Outline omodel, Options opt, ErrorHandler errorHandler) throws SAXException {
        JCodeModel codeModel = omodel.getCodeModel();

        Map<JDefinedClass, JDefinedClass> mixins = new HashMap<>();
        for (ClassOutline classOutline : omodel.getClasses()) {
            try {
                JDefinedClass implClass = classOutline.implClass;
                String newClassName = implClass.name() + "MixIn";
                JDefinedClass newClass = codeModel._class(pckg + "." + newClassName);
                mixins.put(implClass, newClass);

//                TreeMap<String, FieldOutline> sortedByName = new TreeMap<>();
//                for (FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
//                    JFieldVar var = unwrap(fieldOutline);
//                    String name = var.name();
//                    sortedByName.put(hashName(name), fieldOutline);
//                }

                Set<String> names = new HashSet<>();
                for (FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
                    CPropertyInfo propertyInfo = fieldOutline.getPropertyInfo();
                    JFieldVar var = unwrap(fieldOutline);
                    String name = var.name();
                    JFieldVar field = newClass.field(JMod.PUBLIC, fieldOutline.getRawType(), name);
                    field.annotate(JsonProperty.class).param("value", shortening(names, name, propertyInfo.isCollection()));
                }

            } catch (JClassAlreadyExistsException ex) {
                ex.printStackTrace();
            }
        }
        try {
            // start class definition
            JDefinedClass clazz = codeModel._class(pckg + ".ShortinsModule");
            clazz._extends(SimpleModule.class);
            final JMethod method = clazz.method(JMod.PUBLIC, void.class, "setupModule");
            method.annotate(Override.class);
            final JVar ctx = method.param(SetupContext.class, "ctx");
            final JBlock body = method.body();
            for (Map.Entry<JDefinedClass, JDefinedClass> mixin : mixins.entrySet()) {
                body.invoke(ctx, "setMixInAnnotations").arg(JExpr.dotclass(mixin.getKey())).arg(JExpr.dotclass(mixin.getValue()));
            }
        } catch (JClassAlreadyExistsException ex) {
            ex.printStackTrace();
        }
        return true;
    }

//    private String hashName(String name) {
//        try {
//            MessageDigest md = MessageDigest.getInstance("SHA-256");
//            md.update(name.getBytes("UTF-8")); // Change this to "UTF-16" if needed
//            byte[] digest = md.digest();
//            String ret = String.format("%064x", new BigInteger(1, digest));
//            ret += name.length();
//            ret += numberOfUpperCase(name);
//            System.err.println(name + "->" + ret);
//            return ret;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return name;
//    }
//
//    private int numberOfUpperCase(String name) {
//        int count = 0;
//        for (int i = 0; i < name.length(); i++) {
//            if (Character.isUpperCase(name.charAt(i))) count++;
//        }
//        return count;
//    }

    private static final Pattern PATTERN = Pattern.compile("(\\d+)$");

    static String shortening(Set<String> collisions, String name, boolean collection) {
        StringBuilder ending = new StringBuilder();

        Matcher matcher = PATTERN.matcher(name);
        if (matcher.find()) {
            final String group = matcher.group();
            ending.append(group);
            name = name.substring(0, name.length() - group.length());
        }
        if (collection) {
            //TODO: if configure to add s for collection
            ending.insert(0, "s");
            if (name.endsWith("s")) {
                name = name.substring(0, name.length() - 1);
            }
        }

        if (name.toLowerCase().endsWith("id")) {
            ending.insert(0, "id");
            name = name.substring(0, name.length() - 2);
        }


        Map<Integer, Integer> indexes = new HashMap<>();
        StringBuilder builder = new StringBuilder(name.length());
        builder.append(Character.toLowerCase(name.charAt(0)));
        indexes.put(0, 0);
        for (int i = 1; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                indexes.put(i, -1);
                builder.append(Character.toLowerCase(c));
            }
        }

        builder.append(ending);
        String ret = builder.toString();
        int index = 0;
        for (int i = 1; collisions.contains(ret) && ++index < name.length(); i++) {
            int value = indexes.getOrDefault(index, index);
            if (value == -1) continue;
            char c = name.charAt(value);
            builder.insert(i, c);
            ret = builder.toString();
        }
        ret = builder.toString();
        collisions.add(ret);
        System.err.println(name + "  ---> " + ret);
        return ret;
    }
}
