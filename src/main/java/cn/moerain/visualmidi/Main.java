package cn.moerain.visualmidi;

import javax.swing.*;
import java.awt.*;
import java.util.Locale; // 新增

public class Main {
    public static void main(String[] args) {

        // 强制默认语言为简体中文，确保优先加载 messages_zh_CN.properties
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE);

        /* 1. 选一个能显示中文的字体（兼容 Windows / Linux / macOS） */
        Font chineseFont = pickChineseFont();

        /* 2. 一次性应用到所有常用控件 */
        String[] keys = {
            "Button.font", "ToggleButton.font", "RadioButton.font", "CheckBox.font",
            "ColorChooser.font", "ComboBox.font", "Label.font", "List.font",
            "MenuBar.font", "MenuItem.font", "Menu.font", "PopupMenu.font",
            "OptionPane.font", "Panel.font", "ProgressBar.font", "ScrollPane.font",
            "Table.font", "TableHeader.font", "TextField.font", "PasswordField.font",
            "TextArea.font", "TextPane.font", "EditorPane.font", "TitledBorder.font",
            "ToolBar.font", "ToolTip.font", "Tree.font"
        };
        for (String key : keys) {
            UIManager.put(key, chineseFont);
        }

        /* 3. 启动界面 */
        SwingUtilities.invokeLater(() -> new VisualMIDIApp().setVisible(true));
    }

    /** 按优先级返回一个支持中文的字体；都失败则退回默认逻辑字体。 */
    private static Font pickChineseFont() {
        String[] candidates = {
            "Microsoft YaHei",          // Win
            "Noto Sans CJK SC",         // Linux/macOS 发行版常见
            "WenQuanYi Micro Hei",      // 老 Linux
            "SimSun",                   // Win 宋体
            "Dialog"                    // Java 自带最后稻草
        };
        for (String name : candidates) {
            Font f = new Font(name, Font.PLAIN, 12);
            if (f.canDisplayUpTo("\u4e2d\u6587") == -1) {
                return f;
            }
        }
        return new Font("Dialog", Font.PLAIN, 12);
    }
}