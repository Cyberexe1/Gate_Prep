-- ===================================================================== accounts
create table users (
    id            uuid primary key,
    email         varchar(255) not null unique,
    display_name  varchar(80)  not null,
    password_hash varchar(100) not null,
    created_at    timestamptz  not null default now()
);

create table refresh_tokens (
    id         uuid primary key,
    user_id    uuid         not null references users (id) on delete cascade,
    token_hash char(64)     not null unique,
    expires_at timestamptz  not null,
    revoked    boolean      not null default false,
    created_at timestamptz  not null default now()
);
create index refresh_tokens_user_idx on refresh_tokens (user_id);

-- ===================================================================== content
create table subjects (
    id             varchar(40)  primary key,
    name           varchar(120) not null,
    short_name     varchar(40)  not null,
    emoji          varchar(16)  not null default '',
    c1             varchar(9)   not null,
    c2             varchar(9)   not null,
    position       int          not null,
    question_count int          not null default 0
);

create table modules (
    id             varchar(120) primary key,
    subject_id     varchar(40)  not null references subjects (id),
    name           varchar(200) not null,
    position       int          not null,
    question_count int          not null default 0
);

create table mock_tests (
    id             varchar(20) primary key,
    n              int         not null,
    title          varchar(80) not null,
    question_count int         not null,
    marks          int         not null,
    minutes        int         not null,
    c1             varchar(9)  not null,
    c2             varchar(9)  not null,
    emoji          varchar(16) not null default '',
    sections       jsonb       not null,
    nat_count      int         not null default 0
);

create table questions (
    id        varchar(100) primary key,
    module_id varchar(120) references modules (id),
    mock_id   varchar(20)  references mock_tests (id),
    position  int          not null,
    year      int          not null default 0,
    label     text         not null default '',
    qno       varchar(20)  not null default '',
    marks     int          not null default 0,
    type      varchar(8)   not null default '',
    answer    text         not null default '',
    lo        double precision,
    hi        double precision,
    has_sol   boolean      not null default false,
    mode      varchar(8)   not null default 'image',
    src       varchar(10)  not null default '',
    exam      text         not null default '',
    stem      jsonb,
    options   jsonb,
    constraint questions_owner check (module_id is not null or mock_id is not null)
);
create index questions_module_idx on questions (module_id, position);
create index questions_mock_idx on questions (mock_id, position);

-- ===================================================================== per-user data
-- Every practice attempt is kept. The table grows the fastest, so it is partitioned by month on created_at.
-- (The primary key of a partitioned table must contain the partition key.)
create table attempts (
    user_id     uuid         not null references users (id) on delete cascade,
    question_id varchar(100) not null,
    selection   varchar(60)  not null,
    ok          boolean      not null,
    module_id   varchar(120) not null,
    created_at  timestamptz  not null default now(),
    primary key (user_id, question_id, created_at)
) partition by range (created_at);

create table attempts_default partition of attempts default;

create or replace function ensure_attempts_partition(d date) returns void
language plpgsql as $$
declare
    start_d date := date_trunc('month', d)::date;
    end_d   date := (date_trunc('month', d) + interval '1 month')::date;
    part    text := 'attempts_' || to_char(start_d, 'YYYY_MM');
begin
    if to_regclass(part) is null then
        execute format('create table %I partition of attempts for values from (%L) to (%L)', part, start_d, end_d);
    end if;
end $$;

select ensure_attempts_partition((current_date - interval '1 month')::date);
select ensure_attempts_partition(current_date);
select ensure_attempts_partition((current_date + interval '1 month')::date);
select ensure_attempts_partition((current_date + interval '2 month')::date);

create table viewed (
    user_id     uuid         not null references users (id) on delete cascade,
    question_id varchar(100) not null,
    module_id   varchar(120) not null,
    primary key (user_id, question_id)
);

create table user_state (
    user_id      uuid primary key references users (id) on delete cascade,
    positions    jsonb        not null default '{}'::jsonb,
    last_subject varchar(40),
    last_module  varchar(120),
    last_ts      bigint       not null default 0
);

create table mock_sessions (
    id          uuid primary key,
    user_id     uuid         not null references users (id) on delete cascade,
    mock_id     varchar(20)  not null references mock_tests (id),
    started_at  timestamptz  not null,
    duration_ms bigint       not null,
    answers     jsonb        not null default '{}'::jsonb,
    review      jsonb        not null default '[]'::jsonb,
    visited     jsonb        not null default '[]'::jsonb,
    position    int          not null default 0,
    finished    boolean      not null default false,
    finished_at timestamptz,
    score       numeric(7, 2) not null default 0,
    correct     int          not null default 0,
    wrong       int          not null default 0,
    skipped     int          not null default 0,
    ga_score    numeric(7, 2) not null default 0,
    cs_score    numeric(7, 2) not null default 0
);
create index mock_sessions_user_idx on mock_sessions (user_id, mock_id, started_at desc);
create index mock_sessions_rank_idx on mock_sessions (mock_id, score desc) where finished;
