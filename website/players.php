<?php

require_once __DIR__ . '/vendor/autoload.php';

use Carbon\Carbon;
use Cocur\HumanDate\HumanDate;

$config = require '/var/creds/creds.php';
$db = $config['database'];

$conn = mysqli_connect($db['host'], $db['user'], $db['password'], $db['dbname']);
if (!$conn) {
    die('Connection failed: ' . mysqli_connect_error());
}

$offset = (int)$_GET['offset'];

$sql = "select nick, wins, losses, rating from profiles where wins != 0 || losses != 0 ORDER BY wins desc, (wins/losses) desc limit 10 offset $offset";

$response = array();

$result = mysqli_query($conn, $sql);
while ($row = mysqli_fetch_assoc($result)) {
    $name = $row['nick'];
    $wins = $row['wins'];
    $losses = $row['losses'];
    $rating = $row['rating'];
    array_push($response, array("nick" => $name, "wins" => $wins, "losses" => $losses, "rating" => $rating));
}

header("Content-Type: application/json");
echo json_encode($response);

mysqli_close($conn);

?>

