# Flunkydom

*The place where flunkies are*

This web-application contains all your AI servants (= flunkies) in one place. 
You can setup several `agents` which can act independently (like AutoGpt) to achieve `goals`. To reach 
these `goals`, they can use `tools` and `embeddings`.

The application is designed to work on multiple goals in parallel and also supports longer-running 
flows (agents can wait for something, so flows can take days or more to finish. 
Think of something like `Write a tweet about X every 48 hours`)

## Screenshots

### Goals Overview

![Goals](imgs/goals.png)

### Embeddings Overview

![Embeddings](imgs/embeddings.png)

## Details

## Agents

* General-purpose agent (similar to AutoGPT)
* others planned (for researching topics, for writing etc)

## Embeddings

embeddings are created via openai API, stored in postgres using [pgvector](https://github.com/pgvector/pgvector) extension


## Tools

* WeatherApi
* Zapier
* serpapi (answerbox/snippet only)
* HomeAssistant (activation of scenes)
* Wait
* ChatGpt

## Todo

* Configurable Agents: create named agents that have a set of active tools (configurable).
  * you can easily have one agent for your mails and one for twitter for example
* Other type of agents: currently, only one agent-type is implemented
* fix setup issues (secrets have to be in DB on first startup already *doh*)

## Resources

* https://blog.scottlogic.com/2023/05/04/langchain-mini.html
* https://supabase.com/blog/openai-embeddings-postgres-vector
* 
