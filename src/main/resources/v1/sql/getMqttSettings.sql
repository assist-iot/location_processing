select
  id,
  username,
  password,
  host,
  port,
  output_json_format_id
from
  mqtt_settings
where
  mqtt_settings.id = ?;
