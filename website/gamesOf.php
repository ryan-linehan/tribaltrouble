<?php

include 'common.php';

require_once __DIR__ . '/vendor/autoload.php';

use Carbon\Carbon;
use Cocur\HumanDate\HumanDate;

$config = require '/var/creds/creds.php';
$db = $config['database'];

$conn = mysqli_connect($db['host'], $db['user'], $db['password'], $db['dbname']);
if (!$conn) {
    die('Connection failed: ' . mysqli_connect_error());
}

$n = $_GET['name'];
$offset = (int)$_GET['offset'];

$sql = "select * from games where player1_name='$n' or player2_name='$n' or player3_name='$n' or player4_name='$n' or player5_name='$n' or player6_name='$n' and status='completed' order by time_start desc limit 10 offset $offset";

$response = array();
$result = mysqli_query($conn, $sql);
while ($row = mysqli_fetch_assoc($result)) {
    $id = $row['id'];
    $file_exists = file_exists("/var/games/$id");
    $humanDate = new HumanDate();
    $time = $row['time_stop'];
    $last = $humanDate->transform($time);
    if ($last == 'Today') {
        $date = Carbon::parse($time);
        $diff = $date->diffForHumans();
        $htime = "$last $diff";
    }
    $winner = $row['winner'];
    $names = [];
    $losers = [];
    for ($i = 1; $i <= 8; $i++) {
        $team = $row["player${i}_team"];
        $name = $row["player${i}_name"];
        if ($team == $winner) {
            array_push($names, $name);
        } elseif ($name != '') {
            array_push($losers, $name);
        }
    }
    if ($names != []) {
        echo "<i>$htime</i>: ";
        foreach ($names as $name) {
            echo showPlayer($name);
            echo " ";
        }
        echo " won";
        if ($losers != []) {
            echo " against ";
            foreach ($losers as $name) {
                echo showPlayer($name);
                echo " ";
            }
        }
        if ($file_exists) {
            echo " <a href='watch.html#$id' target='_blank'>watch here</a>";
        }
        echo "</br>";
    }

    $row['htime'] = $last;

    $id = $row['id'];
    $row['watchable'] = file_exists("/var/games/$id");

    array_push($response, $row);
}

header("Content-Type: application/json");
echo json_encode($response);

mysqli_close($conn);

?>

