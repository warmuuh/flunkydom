create extension if not exists vector;

create table IF NOT EXISTS scheduled_tasks
(
    task_name            text                     not null,
    task_instance        text                     not null,
    task_data            bytea,
    execution_time       timestamp with time zone not null,
    picked               BOOLEAN                  not null,
    picked_by            text,
    last_success         timestamp with time zone,
    last_failure         timestamp with time zone,
    consecutive_failures INT,
    last_heartbeat       timestamp with time zone,
    version              BIGINT                   not null,
    PRIMARY KEY (task_name, task_instance)
);

CREATE INDEX IF NOT EXISTS execution_time_idx ON scheduled_tasks (execution_time);
CREATE INDEX IF NOT EXISTS last_heartbeat_idx ON scheduled_tasks (last_heartbeat);



create table IF NOT EXISTS goals
(
    goal_id       text                     not null,
    input_query   text,
    creation_time timestamp with time zone not null,
    finish_time   timestamp with time zone,
    status        text                     not null,
    result        text,
    log           text,
    steps         INT,
    agent         text,
    artifacts     bytea,
    PRIMARY KEY (goal_id)
);

CREATE INDEX IF NOT EXISTS status_idx ON goals (status);



create table IF NOT EXISTS embeddings
(
    id        text primary key,
    title     text,
    content   text,
    embedding vector(1536)
);

-- create or replace function match_embeddings (
--     query_embedding vector(1536),
--     match_threshold float,
--     match_count int
-- )
--     returns table (
--                       id bigint,
--                       content text,
--                       similarity float
--                   )
--     language sql stable
-- as $$
-- select
--     embeddings.id,
--     embeddings.content,
--     1 - (embeddings.embedding <=> query_embedding) as similarity
-- from embeddings
-- where 1 - (embeddings.embedding <=> query_embedding) > match_threshold
-- order by similarity desc
-- limit match_count
-- $$;
--
create index on embeddings
    using ivfflat (embedding vector_cosine_ops)
    with (lists = 100);



create table IF NOT EXISTS tool_cfgs
(
    id          text primary key,
    config_json text
);



create table IF NOT EXISTS agents
(
    id             text not null,
    agent_template text,
    name           text not null,
    active_tools   text,
    PRIMARY KEY (id)
);
