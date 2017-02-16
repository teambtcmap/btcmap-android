create table places (
    _id integer primary key,
    _updated_at integer,
    latitude real,
    longitude real,
    name text,
    description text,
    phone text,
    website text,
    amenity text,
    opening_hours text,
    address text,
    visible boolean
);

create table currencies (
    _id integer primary key,
    name text,
    code text,
    crypto boolean not null
);

create table currencies_places (
    _id integer primary key,
    currency_id integer,
    place_id integer
);