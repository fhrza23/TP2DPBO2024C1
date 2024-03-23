import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.sql.*;

public class Menu extends JFrame{
    public static void main(String[] args) {
        // buat object window
        Menu window = new Menu();

        // atur ukuran window
        window.setSize(480, 560);
        // letakkan window di tengah layar
        window.setLocationRelativeTo(null);
        // isi window
        window.setContentPane(window.mainPanel);
        // ubah warna background
        window.getContentPane().setBackground(Color.white);
        // tampilkan window
        window.setVisible(true);
        // agar program ikut berhenti saat window diclose
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // index baris yang diklik
    private int selectedIndex = -1;
    // list untuk menampung semua mahasiswa
    private ArrayList<Mahasiswa> listMahasiswa;
    private Database database;
    private JPanel mainPanel;
    private JTextField nimField;
    private JTextField namaField;
    private JTable mahasiswaTable;
    private JButton addUpdateButton;
    private JButton cancelButton;
    private JComboBox jenisKelaminComboBox;
    private JButton deleteButton;
    private JLabel titleLabel;
    private JLabel nimLabel;
    private JLabel namaLabel;
    private JLabel jenisKelaminLabel;

    // constructor
    public Menu() {
        // inisialisasi listMahasiswa
        listMahasiswa = new ArrayList<>();

        database = new Database();

        // isi tabel mahasiswa
        mahasiswaTable.setModel(setTable());

        // ubah styling title
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));

        // atur isi combo box
        String[] jenisKelaminData = {"", "Laki-laki", "Perempuan"};
        jenisKelaminComboBox.setModel(new DefaultComboBoxModel(jenisKelaminData));

        // sembunyikan button delete
        deleteButton.setVisible(false);

        // saat tombol add/update ditekan
        addUpdateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedIndex == -1) {
                    insertData();
                } else {
                    updateData();
                }
            }
        });
        // saat tombol delete ditekan
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedIndex >= 0) {
                    deleteData();
                }
            }
        });
        // saat tombol cancel ditekan
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // saat tombol
                clearForm();
            }
        });
        // saat salah satu baris tabel ditekan
        mahasiswaTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // ubah selectedIndex menjadi baris tabel yang diklik
                selectedIndex = mahasiswaTable.getSelectedRow();

                // simpan value textfield dan combo box
                String selectedNim = mahasiswaTable.getModel().getValueAt(selectedIndex, 1).toString();
                String selectedNama = mahasiswaTable.getModel().getValueAt(selectedIndex, 2).toString();
                String selectedJenisKelamin = mahasiswaTable.getModel().getValueAt(selectedIndex, 3).toString();

                // ubah isi textfield dan combo box
                nimField.setText(selectedNim);
                namaField.setText(selectedNama);
                jenisKelaminComboBox.setSelectedItem(selectedJenisKelamin);

                // ubah button "Add" menjadi "Update"
                addUpdateButton.setText("Update");
                // tampilkan button delete
                deleteButton.setVisible(true);
            }
        });
    }

    public final DefaultTableModel setTable() {
        // tentukan kolom tabel
        Object[] column = {"No", "NIM", "Nama", "Jenis Kelamin"};

        // buat objek tabel dengan kolom yang sudah dibuat
        DefaultTableModel temp = new DefaultTableModel(null, column);

        try {

            ResultSet resultSet = database.selectQuery("SELECT * FROM mahasiswa");

            int i = 0;
            while (resultSet.next()){
                Object[] row = new Object[4];
                row[0] = i + 1;
                row[1] = resultSet.getString("nim");
                row[2] = resultSet.getString("nama");
                row[3] = resultSet.getString("jenis_kelamin");

                temp.addRow(row);
                i++;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return temp;
    }

    public void insertData() {
        // ambil value dari textfield dan combobox
        String nim = nimField.getText();
        String nama = namaField.getText();
        String jenisKelamin = jenisKelaminComboBox.getSelectedItem().toString();

        if (nim.isEmpty() || nama.isEmpty() || jenisKelamin.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Tidak Boleh Ada Data Kosong");
            return;
        }

        // cek apakah nim atau nama sudah ada di database
        String checkExistingQuery = "SELECT COUNT(*) FROM mahasiswa WHERE nim = '" + nim + "'";
        ResultSet resultSet = null;
        try {
            resultSet = database.selectQuery(checkExistingQuery);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                if (count > 0) {
                    // Jika nim atau nama sudah ada, tampilkan pesan error
                    JOptionPane.showMessageDialog(null, "NIM atau Nama sudah ada di database!");
                    return; // Stop the update process
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // tambahkan data ke dalam DB
        String sql = "INSERT INTO mahasiswa VALUES (null, '" + nim + "', '" + nama + "', '" + jenisKelamin + "')";
        database.insertUpdateDeleteQuery(sql);

        // update tabel
        mahasiswaTable.setModel(setTable());

        // bersihkan form
        clearForm();

        // feedback
        System.out.println("Insert berhasil!");
        JOptionPane.showMessageDialog(null, "Data berhasil ditambahkan");
    }

    public int getMahasiswaId(String nim, String nama) {
        try {
            String sql = "SELECT id FROM mahasiswa WHERE nim='" + nim + "' OR nama='" + nama + "'";
            ResultSet resultSet = database.selectQuery(sql);

            if (resultSet.next()) {
                return resultSet.getInt("id");
            } else {
                return -1; // Return -1 if no matching record found
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateData() {
        // ambil data dari form
        String nim = nimField.getText();
        String nama = namaField.getText();
        String jenisKelamin = jenisKelaminComboBox.getSelectedItem().toString();

        if (nim.isEmpty() || nama.isEmpty() || jenisKelamin.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Tidak Boleh Ada Data Kosong");
            return;
        }

        // cek apakah nim atau nama sudah ada di database
        String checkExistingQuery = "SELECT COUNT(*) FROM mahasiswa WHERE nama = '" + nama + "'";
        ResultSet resultSet = null;
        try {
            resultSet = database.selectQuery(checkExistingQuery);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                if (count > 0) {
                    // Jika nim atau nama sudah ada, tampilkan pesan error
                    JOptionPane.showMessageDialog(null, "Nama sudah ada di database!");
                    return; // Stop the update process
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        int selectedRowIndex = mahasiswaTable.getSelectedRow();
        if (selectedRowIndex == -1) {
            return; // Stop the update process if no row is selected
        }
        String updateNama = (String) mahasiswaTable.getValueAt(selectedRowIndex, 2);
        String updateNim = (String) mahasiswaTable.getValueAt(selectedRowIndex, 1);
        int id = getMahasiswaId(updateNim, updateNama);

        // ubah data mahasiswa di list
        String sql = "UPDATE mahasiswa SET nim = '" + nim + "', nama = '" + nama + "', jenis_kelamin = '" + jenisKelamin + "' WHERE id = '" + nim + "'";
        database.insertUpdateDeleteQuery(sql);

        // update tabel
        mahasiswaTable.setModel(setTable());

        // bersihkan form
        clearForm();

        // feedback
        System.out.println("Update Berhasil!");
        JOptionPane.showMessageDialog(null, "Data berhaasil diubah!");
    }

    public void deleteData() {

        int selectedRowIndex = mahasiswaTable.getSelectedRow();
        if (selectedRowIndex == -1) {
            return; // Stop the update process if no row is selected
        }
        String nama = (String) mahasiswaTable.getValueAt(selectedRowIndex, 2);
        String nim = (String) mahasiswaTable.getValueAt(selectedRowIndex, 1);
        int id = getMahasiswaId(nim, nama);

        // hapus data dari list
        String sql = "DELETE FROM mahasiswa WHERE id = '" + id + "'";
        database.insertUpdateDeleteQuery(sql);
        System.out.println("Mahasiswa dengan ID " + id + " telah dihapus dari database.");

        // update tabel
        mahasiswaTable.setModel(setTable());

        // bersihkan form
        clearForm();

        // feedback
        System.out.println("Delete berhasil!");
        JOptionPane.showMessageDialog(null, "Data berhasil dihapus!");
    }



    public void clearForm() {
        // kosongkan semua texfield dan combo box
        nimField.setText("");
        namaField.setText("");
        jenisKelaminComboBox.setSelectedItem("");

        // ubah button "Update" menjadi "Add"
        addUpdateButton.setText("Add");
        // sembunyikan button delete
        deleteButton.setVisible(false);
        // ubah selectedIndex menjadi -1 (tidak ada baris yang dipilih)
        selectedIndex = -1;
    }

}
