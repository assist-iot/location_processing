select
  id,
  name,
  mqtt_input_settings_id,
  mqtt_output_settings_id,
  sql
from
  queries
where
  name = ?;
