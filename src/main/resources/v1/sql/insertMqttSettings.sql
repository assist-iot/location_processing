insert into
  mqtt_settings (
    id,
    username,
    password,
    host,
    port,
    output_json_format_id,
    query_id
  )
values
  (?, ?, ?, ?, ?, ?, ?);
