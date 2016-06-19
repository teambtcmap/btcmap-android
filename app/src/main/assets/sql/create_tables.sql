CREATE TABLE merchants (
    _id integer primary key,
    _created_at integer,
    _updated_at integer,
    latitude real,
    longitude real,
    name text,
    description text,
    phone text,
    website text,
    amenity text,
    opening_hours text,
    address text
);

CREATE TABLE currencies (
    _id integer primary key,
    _created_at integer,
    _updated_at integer,
    name text,
    code text,
    crypto boolean not null,
    show_on_map boolean not null default 1
);

CREATE TABLE currencies_merchants (
    _id integer primary key,
    _created_at integer,
    _updated_at integer,
    currency_id integer,
    merchant_id integer
);

CREATE TABLE exchange_rates (
    _id integer primary key,
    _created_at integer,
    _updated_at integer,
    currency text,
    base_currency text,
    value real
);