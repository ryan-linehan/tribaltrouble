<?php

function showPlayer($name) {
    return "</b><a href='index.html#player#$name'>$name</a></b>";
}

function showGame($name, $gid) {
    return "<i>$name</i><a href='watch.html#$gid' target='new'>Watch Here!</a>";
}
