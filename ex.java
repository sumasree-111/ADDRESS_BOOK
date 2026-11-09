
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.io.File;
import java.util.Locale;

public class ex {

    private static final String DB_URL = "jdbc:sqlite:addressly.db";
    private static final String FONT_FILE = "Amsterdam Two.ttf";

    private static Connection db;
    private static JFrame frame;
    private static JPanel root, content;
    private static boolean darkMode = true;
    private static String currentPage = "View Customer";

    private static Color bg, sidebar, card, primary, fg, muted, border;
    private static Font titleFont;

    private static JTextField nameField, phoneField, emailField;
    private static JTextField houseField, streetField, cityField, pinField;
    private static JCheckBox defaultBox;

    private static int editingCustomerId = -1;
    private static int editingAddressId = -1;

    private static final String[] COLUMNS = {
        "Customer Name", "Phone", "Email", "House No.",
        "Street", "City", "PIN Code", "Default", "⋮"
    };

    private static class Row {
        int customerId, addressId;

        Row(int c, int a) {
            customerId = c;
            addressId = a;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Class.forName("org.sqlite.JDBC");
                connectDatabase();
                loadFont();
                setTheme();

                frame = new JFrame("Addressly - Customer Address Book");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1200, 760);
                frame.setMinimumSize(new Dimension(950, 620));
                frame.setLocationRelativeTo(null);

                root = new JPanel(new BorderLayout());
                frame.setContentPane(root);

                showPage("View Customer");
                frame.setVisible(true);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                    null,
                    "Database connection failed:\n" + ex.getMessage(),
                    "Addressly",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    // DATABASE

    private static void connectDatabase() throws SQLException {
        db = DriverManager.getConnection(DB_URL);

        try (Statement st = db.createStatement()) {
            st.executeUpdate("PRAGMA foreign_keys = ON");

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS customers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "phone TEXT NOT NULL UNIQUE," +
                "email TEXT)"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS addresses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "customer_id INTEGER NOT NULL," +
                "house TEXT, street TEXT, city TEXT, pin TEXT," +
                "is_default INTEGER DEFAULT 0," +
                "deleted INTEGER DEFAULT 0," +
                "FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE)"
            );
        }
    }

    // FONT

    private static void loadFont() {
        try {
            File f = new File(FONT_FILE);

            if (f.exists()) {
                Font custom = Font.createFont(Font.TRUETYPE_FONT, f);
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .registerFont(custom);
                titleFont = custom.deriveFont(Font.PLAIN, 34f);
            } else {
                titleFont = new Font("Amsterdam two", Font.BOLD, 30);
            }

        } catch (Exception e) {
            titleFont = new Font("Amsterdam two", Font.BOLD, 30);
        }
    }

    // THEME

    private static void setTheme() {
        if (darkMode) {
            bg = new Color(16, 20, 31);
            sidebar = new Color(20, 26, 39);
            card = new Color(28, 36, 51);
            primary = new Color(155, 130, 230);
            fg = new Color(248, 250, 252);
            muted = new Color(161, 161, 170);
            border = new Color(55, 65, 81);
        } else {
            bg = new Color(246, 245, 242);
            sidebar = new Color(251, 250, 247);
            card = Color.WHITE;
            primary = new Color(137, 116, 182);
            fg = new Color(37, 43, 54);
            muted = new Color(119, 121, 133);
            border = new Color(225, 221, 232);
        }
    }

    // COMMON COMPONENTS

    private static JLabel label(String text, int size, boolean bold) {
        JLabel l = new JLabel(text);
        l.setForeground(fg);
        l.setFont(new Font(
            "SansSerif",
            bold ? Font.BOLD : Font.PLAIN,
            size
        ));
        return l;
    }

    private static JButton button(String text, boolean filled) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        if (filled) {
            b.setBackground(primary);
            b.setForeground(Color.WHITE);
        } else {
            b.setBackground(card);
            b.setForeground(fg);
            b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(9, 15, 9, 15)
            ));
        }

        return b;
    }

    private static JTextField field() {
        JTextField f = new JTextField();
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setForeground(fg);
        f.setBackground(bg);
        f.setCaretColor(fg);
        f.setPreferredSize(new Dimension(260, 39));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border),
            BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));
        return f;
    }

    private static JPanel cardPanel() {
        JPanel p = new JPanel();
        p.setBackground(card);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border),
            BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));
        return p;
    }

    // PAGE NAVIGATION

    private static void showPage(String page) {
        currentPage = page;
        setTheme();

        root.removeAll();
        root.setBackground(bg);
        root.setLayout(new BorderLayout());
        root.add(createSidebar(), BorderLayout.WEST);

        content = new JPanel(new BorderLayout());
        content.setBackground(bg);
        root.add(content, BorderLayout.CENTER);

        if (page.equals("Add Customer")) {
            showForm(false);
        } else if (page.equals("Edit Customer")) {
            showForm(true);
        } else if (page.equals("Search")) {
            showSearch();
        } else if (page.equals("Recycle Bin")) {
            showRecycleBin();
        } else {
            showCustomers();
        }

        root.revalidate();
        root.repaint();
    }

    private static JPanel createSidebar() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(sidebar);
        p.setPreferredSize(new Dimension(235, 0));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, border),
            BorderFactory.createEmptyBorder(24, 15, 20, 15)
        ));

        JLabel title = new JLabel("📍ADDRESSLY");
        title.setFont(titleFont);
        title.setForeground(primary);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(title);

        p.add(Box.createVerticalStrut(7));

        JLabel sub = label("CUSTOMER ADDRESS BOOK", 9, true);
        sub.setForeground(muted);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(sub);

        p.add(Box.createVerticalStrut(38));

        JLabel menu = label("MAIN MENU", 11, true);
        menu.setForeground(muted);
        menu.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(menu);

        p.add(Box.createVerticalStrut(12));

        addNav(p, "＋", "Add Customer");
        addNav(p, "▤", "View Customer");
        addNav(p, "⌕", "Search");

        p.add(Box.createVerticalGlue());

        JButton theme = button(
            darkMode ? "☀  Light Theme" : "☾  Dark Theme",
            false
        );

        theme.setAlignmentX(Component.CENTER_ALIGNMENT);
        theme.setMaximumSize(new Dimension(205, 42));

        theme.addActionListener(e -> {
            darkMode = !darkMode;
            showPage(currentPage);
        });

        p.add(theme);
        p.add(Box.createVerticalStrut(15));

        JLabel foot = label(
            "Simple. Organized. Accessible.", 10, false
        );
        foot.setForeground(muted);
        foot.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(foot);

        return p;
    }

    private static void addNav(JPanel panel, String icon, String page) {
        JButton b = new JButton(icon + "     " + page);
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMaximumSize(new Dimension(205, 43));
        b.setPreferredSize(new Dimension(205, 43));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        boolean selected = currentPage.equals(page) ||
            (currentPage.equals("Edit Customer") &&
             page.equals("View Customer"));

        b.setBackground(selected ? primary : sidebar);
        b.setForeground(selected ? Color.WHITE : fg);
        b.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 5));
        b.addActionListener(e -> showPage(page));

        panel.add(b);
        panel.add(Box.createVerticalStrut(6));
    }

    private static JPanel header(String heading, String description) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 7));
        p.setBackground(bg);
        p.setBorder(BorderFactory.createEmptyBorder(27, 30, 20, 30));

        p.add(label(heading, 24, true));

        JLabel d = label(description, 13, false);
        d.setForeground(muted);
        p.add(d);

        return p;
    }

    // VIEW CUSTOMERS

    private static void showCustomers() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(bg);

        page.add(header(
            "View Customers",
            "All saved customer details and addresses."
        ), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setBackground(bg);
        center.setBorder(BorderFactory.createEmptyBorder(0, 30, 25, 30));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        top.setBackground(bg);

        JButton recycle = button("♻ Recycle Bin", false);
        recycle.addActionListener(e -> showPage("Recycle Bin"));
        top.add(recycle);

        JTable table = createTable();
        loadActiveRows(table, "");

        JScrollPane scroll = new JScrollPane(table);
        styleScroll(scroll);
        installRowMenu(table, false);

        center.add(top, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        page.add(center, BorderLayout.CENTER);
        content.add(page, BorderLayout.CENTER);
    }

    // SEARCH

    private static void showSearch() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(bg);

        page.add(header(
            "Search Customers",
            "Search by name, phone, email or address."
        ), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setBackground(bg);
        center.setBorder(BorderFactory.createEmptyBorder(0, 30, 25, 30));

        JTextField search = field();
        search.setPreferredSize(new Dimension(400, 42));

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(bg);
        bar.add(search, BorderLayout.CENTER);

        JTable table = createTable();
        JScrollPane scroll = new JScrollPane(table);
        styleScroll(scroll);
        installRowMenu(table, false);

        Runnable filter = () ->
            loadActiveRows(table, search.getText().trim());

        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filter.run();
            }

            public void removeUpdate(DocumentEvent e) {
                filter.run();
            }

            public void changedUpdate(DocumentEvent e) {
                filter.run();
            }
        });

        filter.run();

        center.add(bar, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        page.add(center, BorderLayout.CENTER);
        content.add(page, BorderLayout.CENTER);
    }

    // TABLE

    private static JTable createTable() {
        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = new JTable(model);
        styleTable(table);

        table.getColumnModel().getColumn(8).setPreferredWidth(45);
        table.getColumnModel().getColumn(8).setMaxWidth(55);

        return table;
    }

    private static void styleTable(JTable table) {
        table.setRowHeight(39);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setBackground(card);
        table.setForeground(fg);
        table.setSelectionBackground(primary);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(border);
        table.setShowVerticalLines(false);

        // DEFAULT ADDRESS BADGE RENDERER
        table.getColumnModel().getColumn(7)
            .setCellRenderer(new DefaultTableCellRenderer() {

                public Component getTableCellRendererComponent(
                    JTable table, Object value,
                    boolean selected, boolean focus,
                    int row, int column) {

                    JLabel l = (JLabel)
                        super.getTableCellRendererComponent(
                            table, value, selected, focus, row, column
                        );

                    l.setHorizontalAlignment(SwingConstants.CENTER);
                    l.setOpaque(true);

                    if ("★ DEFAULT".equals(value)) {
                        l.setForeground(new Color(55, 190, 125));
                        l.setFont(new Font(
                            "SansSerif", Font.BOLD, 11
                        ));
                        l.setBackground(selected ? primary : card);
                    } else {
                        l.setText("");
                        l.setBackground(selected ? primary : card);
                    }

                    return l;
                }
            });

        JTableHeader h = table.getTableHeader();
        h.setFont(new Font("SansSerif", Font.BOLD, 12));
        h.setBackground(card);
        h.setForeground(fg);
        h.setPreferredSize(new Dimension(0, 40));
    }

    private static void styleScroll(JScrollPane s) {
        s.setBorder(BorderFactory.createLineBorder(border));
        s.getViewport().setBackground(card);
    }

    // LOAD ACTIVE ROWS

    private static void loadActiveRows(JTable table, String query) {
        DefaultTableModel m = (DefaultTableModel) table.getModel();
        m.setRowCount(0);

        String sql =
            "SELECT c.id cid,a.id aid,c.name,c.phone,c.email," +
            "a.house,a.street,a.city,a.pin,a.is_default " +
            "FROM customers c JOIN addresses a ON c.id=a.customer_id " +
            "WHERE a.deleted=0 AND (" +
            "lower(c.name) LIKE ? OR c.phone LIKE ? OR " +
            "lower(coalesce(c.email,'')) LIKE ? OR " +
            "lower(coalesce(a.house,'')) LIKE ? OR " +
            "lower(coalesce(a.street,'')) LIKE ? OR " +
            "lower(coalesce(a.city,'')) LIKE ? OR a.pin LIKE ?) " +
            "ORDER BY c.name COLLATE NOCASE";

        try (PreparedStatement ps = db.prepareStatement(sql)) {
            String q = "%" + query.toLowerCase(Locale.ROOT) + "%";

            for (int i = 1; i <= 7; i++) {
                ps.setString(i, q);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    m.addRow(new Object[] {
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("house"),
                        rs.getString("street"),
                        rs.getString("city"),
                        rs.getString("pin"),

                        // Only default address gets a badge
                        rs.getInt("is_default") == 1
                            ? "★ DEFAULT" : "",

                        "⋮"
                    });

                    table.putClientProperty(
                        "row_" + (m.getRowCount() - 1),
                        new Row(rs.getInt("cid"), rs.getInt("aid"))
                    );
                }
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    // THREE DOT MENU

    private static void installRowMenu(JTable table, boolean recycle) {
        table.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;

                int viewRow = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                if (viewRow < 0) return;

                if (col == 8 || e.getClickCount() == 2) {
                    int row = table.convertRowIndexToModel(viewRow);

                    Object value = table.getClientProperty("row_" + row);
                    if (!(value instanceof Row)) return;

                    Row ref = (Row) value;
                    JPopupMenu menu = new JPopupMenu();

                    if (!recycle) {
                        JMenuItem edit = new JMenuItem("✎  Edit");
                        JMenuItem delete = new JMenuItem("🗑  Delete");

                        edit.addActionListener(a -> {
                            editingCustomerId = ref.customerId;
                            editingAddressId = ref.addressId;
                            showPage("Edit Customer");
                        });

                        delete.addActionListener(a -> moveToBin(ref));

                        menu.add(edit);
                        menu.add(delete);

                    } else {
                        JMenuItem restore = new JMenuItem("↶  Restore");
                        JMenuItem permanent =
                            new JMenuItem("✕  Delete Permanently");

                        restore.addActionListener(a -> restoreAddress(ref));
                        permanent.addActionListener(
                            a -> permanentlyDelete(ref)
                        );

                        menu.add(restore);
                        menu.add(permanent);
                    }

                    menu.show(table, e.getX(), e.getY());
                }
            }
        });
    }

    // RECYCLE BIN

    private static void moveToBin(Row ref) {
        int answer = JOptionPane.showConfirmDialog(
            frame,
            "Move this address to Recycle Bin?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (answer != JOptionPane.YES_OPTION) return;

        try (PreparedStatement ps = db.prepareStatement(
            "UPDATE addresses SET deleted=1 WHERE id=?"
        )) {
            ps.setInt(1, ref.addressId);
            ps.executeUpdate();

            showPage(currentPage);

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private static void showRecycleBin() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(bg);

        page.add(header(
            "Recycle Bin",
            "Restore deleted addresses or permanently remove them."
        ), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(bg);
        center.setBorder(BorderFactory.createEmptyBorder(0, 30, 25, 30));

        JTable table = createTable();
        loadDeletedRows(table);
        installRowMenu(table, true);

        JScrollPane scroll = new JScrollPane(table);
        styleScroll(scroll);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        top.setBackground(bg);

        JButton back = button("← Back to Customers", false);
        back.addActionListener(e -> showPage("View Customer"));
        top.add(back);

        center.add(top, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        page.add(center, BorderLayout.CENTER);
        content.add(page, BorderLayout.CENTER);
    }

    private static void loadDeletedRows(JTable table) {
        DefaultTableModel m = (DefaultTableModel) table.getModel();
        m.setRowCount(0);

        String sql =
            "SELECT c.id cid,a.id aid,c.name,c.phone,c.email," +
            "a.house,a.street,a.city,a.pin,a.is_default " +
            "FROM customers c JOIN addresses a ON c.id=a.customer_id " +
            "WHERE a.deleted=1 ORDER BY a.id DESC";

        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                m.addRow(new Object[] {
                    rs.getString("name"),
                    rs.getString("phone"),
                    rs.getString("email"),
                    rs.getString("house"),
                    rs.getString("street"),
                    rs.getString("city"),
                    rs.getString("pin"),
                    rs.getInt("is_default") == 1
                        ? "★ DEFAULT" : "",
                    "⋮"
                });

                table.putClientProperty(
                    "row_" + (m.getRowCount() - 1),
                    new Row(rs.getInt("cid"), rs.getInt("aid"))
                );
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private static void restoreAddress(Row ref) {
        try (PreparedStatement ps = db.prepareStatement(
            "UPDATE addresses SET deleted=0 WHERE id=?"
        )) {
            ps.setInt(1, ref.addressId);
            ps.executeUpdate();

            showPage("Recycle Bin");

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private static void permanentlyDelete(Row ref) {
        int answer = JOptionPane.showConfirmDialog(
            frame,
            "This address will be permanently deleted. Continue?",
            "Permanent Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (answer != JOptionPane.YES_OPTION) return;

        try (PreparedStatement ps = db.prepareStatement(
            "DELETE FROM addresses WHERE id=? AND deleted=1"
        )) {
            ps.setInt(1, ref.addressId);
            ps.executeUpdate();

            try (PreparedStatement check = db.prepareStatement(
                "DELETE FROM customers WHERE id=? AND NOT EXISTS " +
                "(SELECT 1 FROM addresses WHERE customer_id=?)"
            )) {
                check.setInt(1, ref.customerId);
                check.setInt(2, ref.customerId);
                check.executeUpdate();
            }

            showPage("Recycle Bin");

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    // ADD / EDIT FORM

    private static void showForm(boolean editing) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(bg);

        page.add(header(
            editing ? "Edit Customer" : "Add Customer",
            editing
                ? "Update customer and address information."
                : "Enter customer details and their address."
        ), BorderLayout.NORTH);

        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(bg);

        JPanel form = cardPanel();
        form.setLayout(new GridBagLayout());
        form.setPreferredSize(new Dimension(650, 510));

        nameField = field();
        phoneField = field();
        emailField = field();
        houseField = field();
        streetField = field();
        cityField = field();
        pinField = field();

        defaultBox = new JCheckBox("Set as default address");
        defaultBox.setBackground(card);
        defaultBox.setForeground(fg);
        defaultBox.setFont(new Font("SansSerif", Font.PLAIN, 13));

        if (editing) loadEditValues();

        JTextField[] fields = {
            nameField, phoneField, emailField,
            houseField, streetField, cityField, pinField
        };

        String[] names = {
            "Customer Name", "Phone Number", "Email ID",
            "House Number", "Street", "City", "PIN Code"
        };

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i < fields.length; i++) {
            g.gridy = i;
            g.gridx = 0;
            g.weightx = 0.7;
            form.add(label(names[i], 13, false), g);

            g.gridx = 1;
            g.weightx = 1.3;
            form.add(fields[i], g);
        }

        g.gridx = 1;
        g.gridy = 7;
        form.add(defaultBox, g);

        JButton cancel = button("Cancel", false);
        JButton save = button(
            editing ? "Save Changes" : "Save Customer",
            true
        );

        JPanel actions = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 10, 0)
        );
        actions.setBackground(card);
        actions.add(cancel);
        actions.add(save);

        g.gridx = 0;
        g.gridy = 8;
        g.gridwidth = 2;
        form.add(actions, g);

        cancel.addActionListener(e -> {
            editingCustomerId = -1;
            editingAddressId = -1;
            showPage("View Customer");
        });

        save.addActionListener(e -> saveCustomer(editing));

        outer.add(form);
        page.add(outer, BorderLayout.CENTER);
        content.add(page, BorderLayout.CENTER);
    }

    private static void loadEditValues() {
        String sql =
            "SELECT c.name,c.phone,c.email,a.house,a.street," +
            "a.city,a.pin,a.is_default " +
            "FROM customers c JOIN addresses a ON c.id=a.customer_id " +
            "WHERE c.id=? AND a.id=?";

        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, editingCustomerId);
            ps.setInt(2, editingAddressId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    nameField.setText(rs.getString("name"));
                    phoneField.setText(rs.getString("phone"));
                    emailField.setText(rs.getString("email"));
                    houseField.setText(rs.getString("house"));
                    streetField.setText(rs.getString("street"));
                    cityField.setText(rs.getString("city"));
                    pinField.setText(rs.getString("pin"));

                    defaultBox.setSelected(
                        rs.getInt("is_default") == 1
                    );
                }
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    // SAVE CUSTOMER

    private static void saveCustomer(boolean editing) {
        String n = nameField.getText().trim();
        String p = phoneField.getText().trim();
        String em = emailField.getText().trim();
        String h = houseField.getText().trim();
        String s = streetField.getText().trim();
        String c = cityField.getText().trim();
        String pin = pinField.getText().trim();

        if (n.isEmpty() || p.isEmpty() || h.isEmpty() ||
            s.isEmpty() || c.isEmpty() || pin.isEmpty()) {
            message("Please fill all required fields.");
            return;
        }

        if (!p.matches("\\d{10}")) {
            message("Phone number must contain 10 digits.");
            return;
        }

        if (!pin.matches("\\d{6}")) {
            message("PIN code must contain 6 digits.");
            return;
        }

        if (!em.isEmpty() &&
            !em.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            message("Please enter a valid Email ID.");
            return;
        }

        try {
            db.setAutoCommit(false);
            int customerId;
            int addressId = -1;

            if (editing) {
                try (PreparedStatement ps = db.prepareStatement(
                    "UPDATE customers SET name=?,phone=?,email=? WHERE id=?"
                )) {
                    ps.setString(1, n);
                    ps.setString(2, p);
                    ps.setString(3, em);
                    ps.setInt(4, editingCustomerId);
                    ps.executeUpdate();
                }

                customerId = editingCustomerId;
                addressId = editingAddressId;

                try (PreparedStatement ps = db.prepareStatement(
                    "UPDATE addresses SET house=?,street=?,city=?,pin=? " +
                    "WHERE id=? AND customer_id=?"
                )) {
                    ps.setString(1, h);
                    ps.setString(2, s);
                    ps.setString(3, c);
                    ps.setString(4, pin);
                    ps.setInt(5, addressId);
                    ps.setInt(6, customerId);
                    ps.executeUpdate();
                }

            } else {
                try (PreparedStatement find = db.prepareStatement(
                    "SELECT id FROM customers WHERE phone=?"
                )) {
                    find.setString(1, p);

                    try (ResultSet rs = find.executeQuery()) {
                        if (rs.next()) {
                            customerId = rs.getInt(1);
                        } else {
                            try (PreparedStatement insert =
                                db.prepareStatement(
                                    "INSERT INTO customers(name,phone,email) " +
                                    "VALUES(?,?,?)",
                                    Statement.RETURN_GENERATED_KEYS
                                )) {

                                insert.setString(1, n);
                                insert.setString(2, p);
                                insert.setString(3, em);
                                insert.executeUpdate();

                                try (ResultSet keys =
                                    insert.getGeneratedKeys()) {
                                    if (!keys.next()) {
                                        throw new SQLException(
                                            "Could not create customer."
                                        );
                                    }
                                    customerId = keys.getInt(1);
                                }
                            }
                        }
                    }
                }

                try (PreparedStatement ps = db.prepareStatement(
                    "INSERT INTO addresses(customer_id,house,street,city,pin) " +
                    "VALUES(?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS
                )) {
                    ps.setInt(1, customerId);
                    ps.setString(2, h);
                    ps.setString(3, s);
                    ps.setString(4, c);
                    ps.setString(5, pin);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            addressId = keys.getInt(1);
                        }
                    }
                }
            }

            // DEFAULT ADDRESS HANDLING
            if (defaultBox.isSelected()) {
                try (PreparedStatement ps = db.prepareStatement(
                    "UPDATE addresses SET is_default=CASE " +
                    "WHEN id=? THEN 1 ELSE 0 END WHERE customer_id=?"
                )) {
                    ps.setInt(1, addressId);
                    ps.setInt(2, customerId);
                    ps.executeUpdate();
                }

            } else if (!editing) {
                // First address of a customer becomes default automatically
                try (PreparedStatement ps = db.prepareStatement(
                    "SELECT COUNT(*) FROM addresses " +
                    "WHERE customer_id=? AND is_default=1"
                )) {
                    ps.setInt(1, customerId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            try (PreparedStatement update =
                                db.prepareStatement(
                                    "UPDATE addresses SET is_default=1 WHERE id=?"
                                )) {
                                update.setInt(1, addressId);
                                update.executeUpdate();
                            }
                        }
                    }
                }
            }

            db.commit();
            db.setAutoCommit(true);

            editingCustomerId = -1;
            editingAddressId = -1;

            message(editing
                ? "Customer details updated!"
                : "Customer saved successfully!"
            );

            showPage("View Customer");

        } catch (SQLException ex) {
            try {
                db.rollback();
                db.setAutoCommit(true);
            } catch (SQLException ignored) {}

            if (ex.getMessage() != null &&
                ex.getMessage().toLowerCase().contains("unique")) {
                message("This phone number is already used by another customer.");
            } else {
                showError(ex);
            }
        }
    }

    // MESSAGES

    private static void message(String text) {
        JOptionPane.showMessageDialog(
            frame,
            text,
            "Addressly",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static void showError(Exception ex) {
        JOptionPane.showMessageDialog(
            frame,
            ex.getMessage(),
            "Addressly - Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
