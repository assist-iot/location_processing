package eu.assistiot.locationprocessing.v1.shared

import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import scalikejdbc.ConnectionPool
import scalikejdbc.DataSourceConnectionPool
import scalikejdbc.using

import java.util.Properties
import scala.util.Try

object DatabaseHelper {
  val queriesName = "queries"
  val geolocationName = "geolocation"

  private def propertiesFromConfig(config: Config): Properties = {
    val properties = new Properties()
    config
      .entrySet()
      .forEach(entry => {
        properties.setProperty(entry.getKey, entry.getValue.unwrapped.toString)
      })
    properties
  }

  private def initializeConnectionPool(name: String, configName: String, config: Config): Unit = {
    val properties = propertiesFromConfig(config.getConfig(configName))
    val hikariConfig = new HikariConfig(properties)
    val dataSource = new HikariDataSource(hikariConfig)
    ConnectionPool.add(
      name,
      new DataSourceConnectionPool(dataSource = dataSource, closer = () => dataSource.close())
    )
  }

  private def initializeConnectionPools(config: Config): Unit = {
    initializeConnectionPool(queriesName, "v1.queries-db", config)
    initializeConnectionPool(geolocationName, "v1.geolocation-db", config)
  }

  def closeConnectionPools(): Unit = {
    ConnectionPool.closeAll()
  }

  def initialize(config: Config): Unit = {
    val adminConnectionPoolName = "admin"
    initializeConnectionPool(adminConnectionPoolName, "v1.admin-db", config)

    using(ConnectionPool.borrow(adminConnectionPoolName)) { implicit connection =>
      val statement = connection.createStatement()

      Try(statement.execute("""
          |create database queries
          | with
          | encoding = 'UTF8'
          | lc_collate = 'en_US.utf8'
          | lc_ctype = 'en_US.utf8';
          |""".stripMargin))
      Try(statement.execute("""
          |create user queries_user;
          |""".stripMargin))
      Try(statement.execute("""
          |grant all privileges on database queries to queries_user;
          |""".stripMargin))

      Try(statement.execute("""
          |create database geolocation
          | with
          | encoding = 'UTF8'
          | lc_collate = 'en_US.utf8'
          | lc_ctype = 'en_US.utf8';
          |""".stripMargin))
      Try(statement.execute("""
          |create user geolocation_user;
          |""".stripMargin))
      Try(statement.execute("""
          |grant all privileges on database geolocation to geolocation_user;
          |""".stripMargin))
    }

    closeConnectionPools()
    initializeConnectionPools(config)

    using(ConnectionPool.borrow(queriesName)) { implicit connection =>
      connection.setAutoCommit(false)
      val statement = connection.createStatement()

      statement.execute("""
        |create table if not exists mqtt_settings (
        | id uuid not null,
        | username varchar,
        | password varchar,
        | host varchar not null,
        | port int not null,
        | output_json_format_id uuid,
        | query_id uuid not null,
        | primary key (id)
        |);
        |""".stripMargin)

      statement.execute("""
        |create table if not exists mqtt_topics (
        | id uuid not null,
        | name varchar not null,
        | publish_empty_output boolean,
        | publish_when_id uuid,
        | mqtt_settings_id uuid not null,
        | primary key (id)
        |);
        |""".stripMargin)

      statement.execute("""
        |create table if not exists publish_when_types (
        | id uuid not null default gen_random_uuid(),
        | name varchar not null,
        | primary key (id)
        |);
        |""".stripMargin)

      statement.execute("""
        |create table if not exists mqtt_topics_publish_flags (
        | id uuid not null default gen_random_uuid(),
        | name varchar not null,
        | primary key (id)
        |);
        |""".stripMargin)

      statement.execute("""
        |create table if not exists mqtt_topics_publish_flags_junction (
        | mqtt_topic_id uuid not null,
        | flag_id uuid not null
        |);
        |""".stripMargin)

      statement.execute("""
        |create table if not exists queries (
        | id uuid not null,
        | name varchar not null,
        | mqtt_input_settings_id uuid,
        | mqtt_output_settings_id uuid,
        | sql varchar not null,
        | primary key (id)
        |);
        |""".stripMargin)

      statement.execute("""
        |create table if not exists json_formats (
        | id uuid not null,
        | record_format_id uuid not null,
        | show_header boolean not null,
        | wrap_single_column boolean not null,
        | primary key (id)
        |);
        |""".stripMargin)

      statement.execute("""
        |create table if not exists record_formats (
        | id uuid not null default gen_random_uuid(),
        | name varchar not null,
        | primary key (id)
        |);
        |""".stripMargin)

      connection.commit();

      Try(statement.execute("""
        |alter table mqtt_settings
        | add constraint mqtt_settings_fk_queries
        |  foreign key (query_id)
        |  references queries (id)
        |  on delete cascade
        |  on update cascade
        |  initially deferred;
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |alter table mqtt_settings
        | add constraint mqtt_settings_fk_json_formats
        |  foreign key (output_json_format_id)
        |  references json_formats (id)
        |  on delete set null
        |  on update cascade;
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |alter table mqtt_topics
        | add constraint mqtt_topics_fk_mqtt_settings
        |  foreign key (mqtt_settings_id)
        |  references mqtt_settings (id)
        |  on delete cascade
        |  on update cascade;
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |alter table mqtt_topics
        | add constraint mqtt_topics_fk_publish_when_types
        |  foreign key (publish_when_id)
        |  references publish_when_types (id)
        |  on delete no action
        |  on update cascade;
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |alter table mqtt_topics_publish_flags
        | add constraint mqtt_topics_publish_flags_unique_name
        |  unique (name);
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |alter table mqtt_topics_publish_flags_junction
        | add constraint mqtt_topics_publish_flags_junction_fk_mqtt_topics
        |  foreign key (mqtt_topic_id)
        |  references mqtt_topics (id)
        |  on delete cascade
        |  on update cascade
        |  initially deferred;
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |alter table mqtt_topics_publish_flags_junction
        | add constraint mqtt_topics_publish_flags_junction_fk_publish_flags
        |  foreign key (flag_id)
        |  references mqtt_topics_publish_flags (id)
        |  on delete cascade
        |  on update cascade;
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |alter table mqtt_topics_publish_flags_junction
        | add constraint mqtt_topics_publish_flags_junction_unique_fk_pair
        |  unique (mqtt_topic_id, flag_id);
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |alter table queries
        | add constraint queries_unique_name
        |  unique (name);
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |create unique index queries_name_idx on queries (name);
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |alter table queries
        | add constraint queries_fk_mqtt_settings_input
        |  foreign key (mqtt_input_settings_id)
        |  references mqtt_settings (id)
        |  on delete set null
        |  on update cascade;
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |alter table queries
        | add constraint queries_fk_mqtt_settings_output
        |  foreign key (mqtt_output_settings_id)
        |  references mqtt_settings (id)
        |  on delete set null
        |  on update cascade;
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |alter table json_formats
        | add constraint json_formats_fk_record_formats
        |  foreign key (record_format_id)
        |  references record_formats (id)
        |  on delete no action
        |  on update cascade;
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |alter table publish_when_types
        | add constraint publish_when_types_unique_name
        |  unique (name);
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |alter table record_formats
        | add constraint record_formats_unique_name
        |  unique (name);
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |insert into mqtt_topics_publish_flags (name) values
        | ('QoSAtLeastOnceDelivery'),
        | ('QoSAtMostOnceDelivery'),
        | ('QoSExactlyOnceDelivery'),
        | ('Retain');
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |insert into publish_when_types (name) values
        | ('always'),
        | ('failure'),
        | ('success');
        |""".stripMargin))

      connection.commit();

      Try(statement.execute("""
        |insert into record_formats (name) values
        | ('array'),
        | ('object');
        |""".stripMargin))

      connection.commit();
    }

    using(ConnectionPool.borrow(geolocationName)) { implicit connection =>
      val statement = connection.createStatement()
      statement.execute("create extension if not exists postgis;")
    }
  }
}
