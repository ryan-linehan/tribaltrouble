<?php

$id = $_GET['gid'];
$offset = $_GET['offset'];
$name = "/var/games/$id";
$file = fopen($name, 'r');
if ($file === false) {
    die('Failed to open file');
}
$size = filesize($name);
if ($offset < $size) {
    fseek($file, $offset, SEEK_SET);
    $data = fread($file, $size - $offset);
    echo $data;
}
fclose($file);
