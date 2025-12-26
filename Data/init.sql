-- =========================
-- Database: pentest_final
-- Full idempotent init script (có thể chạy lại nhiều lần an toàn)
-- =========================

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

-- Tạo database nếu chưa tồn tại
CREATE DATABASE IF NOT EXISTS `pentest_final`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `pentest_final`;

-- Xóa bảng cũ nếu đã tồn tại (để đảm bảo cấu trúc mới được áp dụng)
DROP TABLE IF EXISTS `imgs`;

-- Tạo bảng mới với cấu trúc đúng ngay từ đầu
CREATE TABLE `imgs` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `name_by_user` TEXT NOT NULL,
    `name_on_server` TEXT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `name_on_server` (`name_on_server`(255)) USING HASH   -- Giới hạn độ dài cho UNIQUE trên TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Chèn dữ liệu mẫu
-- Sử dụng INSERT IGNORE để nếu có bản ghi trùng id thì bỏ qua (an toàn khi chạy lại)
INSERT IGNORE INTO `imgs` (`id`, `name_by_user`, `name_on_server`) VALUES
(1, 'normal_is_boring', '0adc0220976e5599bfad5322a3b23f8e.jpg'),
(2, 'just_duck', '0ff2756ca177662da9d0b31dc9d2296b.jpg'),
(3, 'no_you_can''t', 'de0683054b8e0244eeea37931c71ac99.jpg'),
(4, 'after_a_day', 'b0dda791f69a23f669d8de39a478dc2e.jpg'),
(5, 'test_upload', 'b896a31c226998343b68d835dca77249.png'),
(6, 'druk_guy', '5fb651086bdbff75457b01c5d9761deb.jpg'),
(7, 'omni_man', '0da44377fc2fdfe97d3eae781b89e0bd.jpg'),
(8, 'pink_girl', '859b7487653efce062d0f3a2d7d8b624.jpg'),
(9, 'that''s_what_she_said', '5fcc6bbf83799fa9f12f4528e3227979.png'),
(10, 'just_flower', '26fd54dd24be859847ab856d3b4150f8.jpg'),
(11, 'i_wanna_hang_myself', '1af4f07ae6cf0d5f4f5a5e61fedee632.jpg'),
(12, 'thurst_2009', '06b963d1f76d0a7b33baaea6f1891d8b.png');

-- Đặt lại giá trị AUTO_INCREMENT bắt đầu từ 13 (cho các bản ghi mới)
ALTER TABLE `imgs` AUTO_INCREMENT = 13;

-- COMMIT không cần thiết vì MySQL tự commit trong script init