select
  mqtt_topics.id as id,
  mqtt_topics.name as name,
  publish_empty_output,
  publish_when_types.name as publish_when
from
  mqtt_topics
    left join publish_when_types on mqtt_topics.publish_when_id = publish_when_types.id
where
  mqtt_topics.mqtt_settings_id = ?;
