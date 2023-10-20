select
  record_formats.name as record_format,
  show_header,
  wrap_single_column
from
  json_formats
    inner join record_formats on json_formats.record_format_id = record_formats.id
where
  json_formats.id = ?;
