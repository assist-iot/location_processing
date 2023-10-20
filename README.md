# Location processing

## Description
The Location Processing enabler aims to provide highly configurable and flexible geofencing capabilities based on location data.
The enabler consists of a Scala application and a Postgres database.

The application is written with the Akka framework. 
It runs user-defined SQL queries against the database.
The incoming data is collected from input streams or HTTP requests; it allows for streaming the query results.
The transferred data is in JSON format. The behavior of the application is configurable through an HTTP interface.
The application streaming capabilities are compatible with the MQTT protocol.

The database is shipped with the Postgis extension.
It stores the geolocation data and the application configuration.

The swagger documentation of the application is available at `/v1/api-docs/swagger.json`.

[**More details can be found in the official ASSIST-IoT documentation.**](https://assist-iot-enablers-documentation.readthedocs.io/en/latest/verticals/self/location_process_enabler.html)

## Installation

### Requirements
- Docker
- Docker Compose

### Development environment
For development, run the following scripts:

```bash
# first terminal

./scripts/dev-env.sh
```
The `dev-env.sh` script starts the Postgres database (with a pgAdmin instance) and the MQTT broker (with a MQTT explorer instance).
The database is accessible at `localhost:5432`.
The pgAdmin instance is accessible at `localhost:5433`.
The MQTT broker is accessible at `localhost:1883`.
The MQTT explorer instance is accessible at `localhost:4000`.
Additionally, one can run the `qgis.sh` script to start a QGIS instance to visualize the geolocation data.

```bash
# second terminal

./scripts/dev-app.sh
```
The `dev-app.sh` script starts the application.
The application is accessible at `localhost:8080`.


### Production environment
To simulate the production environment, run the following scripts:

```bash
# first terminal

./scripts/prod-env.sh
```
The `prod-env.sh` script starts the Postgres database.

```bash
# second terminal

./scripts/prod-app.sh
```
The `prod-app.sh` script starts the application.
The application is accessible at `localhost:8080`.

## Authors and acknowledgment
Przemysław Hołda, Piotr Sowiński, and others.

Systems Research Institute, Polish Academy of Sciences

This work is part of the ASSIST-IoT project that has received funding from the EU’s Horizon 2020 research and innovation programme under grant agreement No 957258.

## License
Location Processing is released under the Apache License 2.0. See the `LICENSE` file for details.
