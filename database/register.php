<?php
include "koneksi.php";

$username = $_POST['username'];
$gmail = $_POST['gmail'];
$password = $_POST['password'];

// Cek apakah email sudah digunakan
$cek = mysqli_query($koneksi, "SELECT * FROM users WHERE gmail='$gmail' OR username='$username'");
if (mysqli_num_rows($cek) > 0) {
    echo json_encode(["success" => false, "message" => "Username atau Email sudah digunakan!"]);
    exit;
}

// Simpan data ke database
$query = "INSERT INTO users (username, gmail, password) VALUES ('$username', '$gmail', '$password')";
if (mysqli_query($koneksi, $query)) {
    echo json_encode([
        "success" => true,
        "message" => "Registrasi berhasil! Silakan login."
    ]);
} else {
    echo json_encode([
        "success" => false,
        "message" => "Gagal registrasi!"
    ]);
}
?>