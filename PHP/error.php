<?php

function return_error($code, $message)
{
    http_response_code($code);
    $error_body = array('code' => $code, 'message' => $message);
    echo json_encode($error_body);
    exit();
}

?>
