<?php

function return_error($code, $message)
{
    http_response_code($code);
    $error_body = array('code' => $code, 'message' => $message);
    echo json_encode($error_body);
    exit();
}

function return_error_with_params($code, $message, $data, $get, $post)
{
    http_response_code($code);
    $error_body = array('code' => $code, 'message' => $message, 'data' => $data, 'get' => $get, 'post' => $post);
    echo json_encode($error_body);
    exit();
}

?>
