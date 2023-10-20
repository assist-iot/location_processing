select
  publish_flags.name as publish_flags
from
  mqtt_topics
    inner join mqtt_topics_publish_flags_junction as junction on mqtt_topics.id = junction.mqtt_topic_id
    inner join mqtt_topics_publish_flags as publish_flags on publish_flags.id = junction.flag_id
where
  mqtt_topics.id = ?;
