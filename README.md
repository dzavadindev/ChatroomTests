# README
This is the integration test suite for the module internet technology.

Currently it contains automatic test for:

- Server login
- Server heartbeat
- Private messages and Broadcasts
- Users list

## Parameters
Parameters can be configured in the testconfig.properties file

| Parameter                  | Description                                                                    |
|----------------------------|--------------------------------------------------------------------------------|
| host                       | Ip address of host to connect to                                               |
| port                       | Port where the chat client is running on                                       |
| ping_time_ms               | Time period (in ms) between ping requests from server                          |
| ping_time_ms_delta_allowed | Maximum allowed time difference (in ms) for ping request as measured by client |

## To run
1. Make sure the server is started (Server.java running)
2. Start the integration test by simply running this program from within IntellIj
