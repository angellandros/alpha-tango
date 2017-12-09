<?php

require_once "error.php";
require_once "dbhandler.php";

$method = $_SERVER['REQUEST_METHOD'];
$input = json_decode(file_get_contents('php://input'), true);

if (json_last_error() != JSON_ERROR_NONE)
{
    return_error(400, 'bad json input');
}

if ($method == 'GET')
{
    if (!isset($input['secret']))
    {
        return_error(400, 'mandatory argument "secret" has no value');
    }
    
    $secret = $input['secret'];
    
}    

?>
