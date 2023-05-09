INSERT INTO tool_cfgs(id, config_json) values ('zapier', '{"token":""}') ON CONFLICT DO NOTHING;
INSERT INTO tool_cfgs(id, config_json) values ('openai', '{"token":""}') ON CONFLICT DO NOTHING;
INSERT INTO tool_cfgs(id, config_json) values ('weatherapi', '{"token":""}') ON CONFLICT DO NOTHING;
INSERT INTO tool_cfgs(id, config_json) values ('serpapi', '{"token":""}') ON CONFLICT DO NOTHING;
INSERT INTO tool_cfgs(id, config_json) values ('hass', '{"token":"", "endpoint":""}') ON CONFLICT DO NOTHING;
