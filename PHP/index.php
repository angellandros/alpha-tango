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
    $handler = db_handler::get_instance();
    
    $result = $handler->read($secret);
    while($row = $result->fetch_assoc())
    {
        $body = array('code' => 200, 'secret' => $secret, 'story' => $row['story']);
        echo json_encode($body);
        exit();
    }
}

if ($method == 'POST')
{
    if (!isset($input['secret']))
    {
        return_error(400, 'mandatory argument "secret" has no value');
    }
    
    if (!isset($input['story']))
    {
        return_error(400, 'mandatory argument "story" has no value');
    }
    
    $secret = $input['secret'];
    $story = $input['story'];
    $handler = db_handler::get_instance();
    
    $result = $handler->insert($secret, $story);
    
    $body = array('code' => 200, 'successful' => true);
    echo json_encode($body);
    exit();
}

?>
