create table places (
    _id integer primary key,
    _updated_at integer not null,
    latitude real not null,
    longitude real not null,
    name text not null,
    description text not null,
    phone text not null,
    website text not null,
    category_id integer not null,
    opening_hours text not null,
    visible boolean not null
);

create table place_categories (
    _id integer primary key,
    name text not null,
    unique (name)
);

create table currencies (
    _id integer primary key,
    name text not null,
    code text not null,
    crypto boolean not null,
    unique (name),
    unique (code)
);

create table currencies_places (
    _id integer primary key,
    currency_id integer not null,
    place_id integer not null,
    unique (currency_id, place_id)
);