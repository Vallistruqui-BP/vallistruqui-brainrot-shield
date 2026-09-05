package com.vallistruqui.brainrotshield;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class CalmPremiumResourceTest {
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    @Test
    public void mainScreenPresentsProtectionAndSetupBeforeAdvancedControls() throws Exception {
        Document layout = parseResource("layout/activity_main.xml");

        int protectionTitle = elementIndex(layout, "id", "@+id/protection_state_title");
        int protectionDetail = elementIndex(layout, "id", "@+id/protection_state_detail");
        int setup = elementIndex(layout, "text", "@string/permissions_title");
        int apps = elementIndex(layout, "text", "@string/apps_title");
        int restrictions = elementIndex(layout, "text", "@string/restrictions_title");
        int pauses = elementIndex(layout, "text", "@string/temporary_pauses_title");
        int administration = elementIndex(layout, "text", "@string/settings_access_title");
        int youtubeHelp = elementIndex(layout, "text", "@string/youtube_controls_help_title");

        assertTrue("The protection headline must be present", protectionTitle >= 0);
        assertTrue("The protection explanation must be present", protectionDetail > protectionTitle);
        assertTrue("Setup must follow the protection summary", setup > protectionDetail);
        assertTrue("Apps must follow setup", apps > setup);
        assertTrue("Restrictions must follow apps", restrictions > apps);
        assertTrue("Temporary pauses must follow restrictions", pauses > restrictions);
        assertTrue("Administration must follow the everyday controls", administration > pauses);
        assertTrue("YouTube troubleshooting must remain secondary", youtubeHelp > administration);
    }

    @Test
    public void redesignPreservesEveryFunctionalViewId() throws Exception {
        Document layout = parseResource("layout/activity_main.xml");
        String[] functionalIds = {
                "accessibility_settings_button",
                "accessibility_status",
                "active_rules_summary",
                "add_time_window_button",
                "change_daily_limit_button",
                "configure_pin_button",
                "daily_limit_switch",
                "daily_limit_value",
                "instagram_switch",
                "installed_apps_status",
                "lock_now_button",
                "open_test_app_button",
                "page_root",
                "pause_limit_button",
                "pause_schedule_button",
                "pause_shorts_button",
                "remove_pin_button",
                "schedule_switch",
                "settings_access_status",
                "shorts_switch",
                "tiktok_switch",
                "time_windows_container",
                "usage_access_button",
                "usage_access_status",
                "youtube_controls_help_button",
                "youtube_switch"
        };

        for (String id : functionalIds) {
            assertTrue(
                    "Missing functional view @+id/" + id,
                    elementIndex(layout, "id", "@+id/" + id) >= 0);
        }
    }

    @Test
    public void scrollingContentDoesNotDrawBehindSystemBars() throws Exception {
        Document layout = parseResource("layout/activity_main.xml");
        Element root = layout.getDocumentElement();

        assertTrue(
                "The scroll container must clip content at its system-bar padding",
                !"false".equals(root.getAttributeNS(ANDROID_NS, "clipToPadding")));
    }

    @Test
    public void heroEyebrowCanGrowAtAccessibilityFontSizes() throws Exception {
        Document layout = parseResource("layout/activity_main.xml");
        Element eyebrow = elementByAttribute(layout, "text", "@string/hero_eyebrow");

        assertTrue("The hero eyebrow must be present", eyebrow != null);
        assertEquals(
                "wrap_content",
                eyebrow.getAttributeNS(ANDROID_NS, "layout_height"));
        assertEquals("48dp", eyebrow.getAttributeNS(ANDROID_NS, "minHeight"));
    }

    @Test
    public void calmCopyRetainsPermissionAndPackageScopeAssurances() throws Exception {
        Document strings = parseResource("values/strings.xml");

        assertTrue(
                "The permission copy must preserve short-form independence",
                stringValue(strings, "permissions_body")
                        .contains("videos cortos sin activar el"));
        assertTrue(
                "The app copy must preserve TikTok package scope",
                stringValue(strings, "apps_body")
                        .contains("paquete oficial de Google Play"));
    }

    @Test
    public void calmPremiumTextAndStatusColorsMeetWcagAaInBothThemes() throws Exception {
        assertThemeContrast("values/colors.xml");
        assertThemeContrast("values-night/colors.xml");
    }

    @Test
    public void legacyNavigationBarKeepsLightSystemIconsVisibleWithoutNewApiAttributes()
            throws Exception {
        assertLegacyNavigationBar("values/colors.xml", "values/themes.xml");
        assertLegacyNavigationBar("values-night/colors.xml", "values-night/themes.xml");
    }

    private static void assertLegacyNavigationBar(String colorsPath, String themesPath)
            throws Exception {
        Map<String, Integer> colors = readColors(colorsPath);
        Integer navigationBar = colors.get("navigation_bar");
        assertTrue("Missing color navigation_bar in " + colorsPath, navigationBar != null);
        double iconContrast = contrastRatio(0xFFFFFF, navigationBar);
        assertTrue(
                "Light system icons on navigation_bar have contrast " + iconContrast,
                iconContrast >= 4.5);

        Document theme = parseResource(themesPath);
        assertTrue(
                themesPath + " must use the compatible navigation bar color",
                "@color/navigation_bar".equals(styleItemValue(theme, "android:navigationBarColor")));
        assertTrue(
                themesPath + " must not expose API 27 attributes to Android 6–8",
                styleItemValue(theme, "android:windowLightNavigationBar") == null);
    }

    private static void assertThemeContrast(String resourcePath) throws Exception {
        Map<String, Integer> colors = readColors(resourcePath);

        assertContrastAtLeast(colors, "ink", "canvas", 4.5);
        assertContrastAtLeast(colors, "muted", "canvas", 4.5);
        assertContrastAtLeast(colors, "ink", "surface", 4.5);
        assertContrastAtLeast(colors, "on_primary", "primary", 4.5);
        assertContrastAtLeast(colors, "status_ok_text", "status_ok_background", 4.5);
        assertContrastAtLeast(
                colors, "status_warning_text", "status_warning_background", 4.5);
    }

    private static Map<String, Integer> readColors(String resourcePath) throws Exception {
        Document colorsDocument = parseResource(resourcePath);
        Map<String, Integer> colors = new HashMap<>();
        NodeList colorsNodes = colorsDocument.getElementsByTagName("color");
        for (int index = 0; index < colorsNodes.getLength(); index++) {
            Element color = (Element) colorsNodes.item(index);
            String value = color.getTextContent().trim();
            if (value.matches("#[0-9A-Fa-f]{6}")) {
                colors.put(color.getAttribute("name"), Integer.parseInt(value.substring(1), 16));
            }
        }
        return colors;
    }

    private static void assertContrastAtLeast(
            Map<String, Integer> colors,
            String foregroundName,
            String backgroundName,
            double minimum) {
        Integer foreground = colors.get(foregroundName);
        Integer background = colors.get(backgroundName);
        assertTrue("Missing color " + foregroundName, foreground != null);
        assertTrue("Missing color " + backgroundName, background != null);
        double contrast = contrastRatio(foreground, background);
        assertTrue(
                foregroundName + " on " + backgroundName + " has contrast " + contrast,
                contrast >= minimum);
    }

    private static double contrastRatio(int first, int second) {
        double firstLuminance = relativeLuminance(first);
        double secondLuminance = relativeLuminance(second);
        return (Math.max(firstLuminance, secondLuminance) + 0.05)
                / (Math.min(firstLuminance, secondLuminance) + 0.05);
    }

    private static double relativeLuminance(int color) {
        double red = linearChannel((color >> 16) & 0xFF);
        double green = linearChannel((color >> 8) & 0xFF);
        double blue = linearChannel(color & 0xFF);
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private static double linearChannel(int channel) {
        double normalized = channel / 255.0;
        return normalized <= 0.04045
                ? normalized / 12.92
                : Math.pow((normalized + 0.055) / 1.055, 2.4);
    }

    private static int elementIndex(Document document, String attribute, String expected) {
        NodeList nodes = document.getElementsByTagName("*");
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (expected.equals(element.getAttributeNS(ANDROID_NS, attribute))) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static Element elementByAttribute(
            Document document, String attribute, String expected) {
        NodeList nodes = document.getElementsByTagName("*");
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (expected.equals(element.getAttributeNS(ANDROID_NS, attribute))) {
                    return element;
                }
            }
        }
        return null;
    }

    private static String styleItemValue(Document document, String itemName) {
        NodeList items = document.getElementsByTagName("item");
        for (int index = 0; index < items.getLength(); index++) {
            Element item = (Element) items.item(index);
            if (itemName.equals(item.getAttribute("name"))) {
                return item.getTextContent().trim();
            }
        }
        return null;
    }

    private static String stringValue(Document document, String stringName) {
        NodeList strings = document.getElementsByTagName("string");
        for (int index = 0; index < strings.getLength(); index++) {
            Element string = (Element) strings.item(index);
            if (stringName.equals(string.getAttribute("name"))) {
                return string.getTextContent().trim();
            }
        }
        throw new AssertionError("Missing string " + stringName);
    }

    private static Document parseResource(String relativePath) throws Exception {
        Path resource = Paths.get("src/main/res").resolve(relativePath);
        if (!Files.exists(resource)) {
            resource = Paths.get("app/src/main/res").resolve(relativePath);
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(resource.toFile());
    }
}
