<?php

require_once "error.php";

class db_handler
{
	private $host;
	private $username;
	private $password;
	private $dbname;
	
	private $mysqli;
	
	private $tablename;
	
	protected static $instance = null;
	
	// default constructor
	// loads connection parameters from ini file
	public function __construct() 
	{
		$conf_array = parse_ini_file("conf.ini");
	
		$this->host = $conf_array['host'];
		$this->username = $conf_array['username'];
		$this->password = $conf_array['password'];
		$this->dbname = $conf_array['dbname'];
		$this->tablename = $conf_array['tablename'];
		
		$this->mysqli = new mysqli($this->host, $this->username, $this->password, $this->dbname);

		// check for connection error
		if ($this->mysqli->connect_errno)
		{
			return_error(500, $this->mysqli->connect_error);
		}
	}
	
	// destructor
	// closes the connection
	public function __destruct()
	{
		$this->mysqli->close();
	}
	
	// no clone!
	protected function __clone()
    {
        //Me not like clones! Me smash clones!
    }
	
	// me singleton!
	public static function get_instance()
    {
        if (!isset(self::$instance)) {
            self::$instance = new self();
        }
        return self::$instance;
    }
	
	// set table name
	public function set_table($tablename)
	{
		$this->tablename = $tablename;
		$_SESSION['tablename'] = $this->tablename;
	}
	
	// get table name
	public function get_table()
	{
		return $this->tablename;
	}
	
	// custome query
	public function query($q)
	{
		return $this->mysqli->query($q);
	}
	
	// read a row by given secret
	public function read($secret)
	{
		$prepared = $this->mysqli->prepare("SELECT * FROM `" . $this->tablename . "` WHERE `secret`=?");
		$prepared->bind_param('s', $secret);
		$prepared->execute();
		$result = $prepared->get_result();
		
		if ($result->num_rows == 0)
		{
		    return_error(404, 'record not found');
		}
		return $result;
	}
	
	// insert a row with secret and story
	public function insert($secret, $story)
	{
        $prepared = $this->mysqli->prepare("INSERT INTO `" . $this->tablename . "` (`secret`, `story`) VALUES (?, ?);");
        $prepared->bind_param('ss', $secret, $story);
		$prepared->execute();
		$result = $prepared->get_result();
		
		if (!($result === TRUE))
		{
		    return_error(403, $this->mysqli->error);
		}
	}
}


?>
