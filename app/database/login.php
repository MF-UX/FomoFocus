<?php
include "koneksi.php";

$loginInput = $_POST['loginInput'];
$password = $_POST['password'];

$query = "SELECT * FROM users WHERE (gmail='$loginInput' OR username='$loginInput') AND password='$password'";
$result = mysqli_query($koneksi, $query);

if (mysqli_num_rows($result) > 0) {
    $user = mysqli_fetch_assoc($result);
    echo json_encode([
        "success" => true,
        "message" => "Login berhasil!",
        "username" => $user['username'],
        "gmail" => $user['gmail'],
        "password" => $user['password']
    ]);
} else {
    echo json_encode([
        "success" => false,
        "message" => "Email/Username atau password salah!"
    ]);
}
?>