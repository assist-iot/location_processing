insert into
  mqtt_topics (
    id,
    name,
    publish_empty_output,
    publish_when_id,
    mqtt_settings_id
  )
values
  (?, ?, ?, ?, ?);
