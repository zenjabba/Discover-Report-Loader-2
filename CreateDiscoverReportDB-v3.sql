--
-- PostgreSQL database dump
--

-- Dumped from database version 9.4.1
-- Dumped by pg_dump version 9.4.1
-- Started on 2015-04-11 00:31:59 AEST

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = 'discoveryreport' AND pid <> pg_backend_pid();
DROP DATABASE discoveryreport2;

--
-- TOC entry 2316 (class 1262 OID 19001)
-- Name: discoveryreport2; Type: DATABASE; Schema: -; Owner: postgres
--

CREATE DATABASE discoveryreport2 WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'C' LC_CTYPE = 'C';


ALTER DATABASE discoveryreport2 OWNER TO postgres;

\connect discoveryreport2

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- TOC entry 6 (class 2615 OID 2200)
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA public;


ALTER SCHEMA public OWNER TO postgres;

--
-- TOC entry 2317 (class 0 OID 0)
-- Dependencies: 6
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS 'standard public schema';


--
-- TOC entry 182 (class 3079 OID 12123)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 2319 (class 0 OID 0)
-- Dependencies: 182
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

--
-- TOC entry 534 (class 1247 OID 19003)
-- Name: status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE status AS ENUM (
    'Active',
    'Deleted',
    'Replaced'
);


ALTER TYPE status OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 172 (class 1259 OID 19009)
-- Name: events; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
--

CREATE TABLE events (
    eventid integer NOT NULL,
    boxeventid character varying(25) NOT NULL,
    type character varying(52),
    ipaddress character varying(15),
    datetime character varying(25),
    boxuserid character varying(24),
    boxitemid character varying(24),
    boxitemversionid character varying(24),
    boxitemtype character varying(24),
    note text
);


ALTER TABLE events OWNER TO postgres;

--
-- TOC entry 173 (class 1259 OID 19015)
-- Name: events_eventid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE events_eventid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE events_eventid_seq OWNER TO postgres;

--
-- TOC entry 2320 (class 0 OID 0)
-- Dependencies: 173
-- Name: events_eventid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE events_eventid_seq OWNED BY events.eventid;


--
-- TOC entry 174 (class 1259 OID 19017)
-- Name: files; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
--

CREATE TABLE files (
    fileid integer NOT NULL,
    boxfileid character varying(24),
    boxfileversionid character varying(24),
    name character varying(256),
    sha character varying(128),
    type character varying(12),
    filesize character varying(12),
    note text,
    boxfolderid character varying(24),
    status status
);


ALTER TABLE files OWNER TO postgres;

--
-- TOC entry 175 (class 1259 OID 19023)
-- Name: files_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE files_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE files_id_seq OWNER TO postgres;

--
-- TOC entry 2321 (class 0 OID 0)
-- Dependencies: 175
-- Name: files_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE files_id_seq OWNED BY files.fileid;


--
-- TOC entry 176 (class 1259 OID 19025)
-- Name: folders; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
--

CREATE TABLE folders (
    folderid integer NOT NULL,
    boxfolderid character varying,
    boxparentfolderid character varying(24),
    foldername character varying(256),
    note text,
    status status
);


ALTER TABLE folders OWNER TO postgres;

--
-- TOC entry 177 (class 1259 OID 19031)
-- Name: folders_folderid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE folders_folderid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE folders_folderid_seq OWNER TO postgres;

--
-- TOC entry 2322 (class 0 OID 0)
-- Dependencies: 177
-- Name: folders_folderid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE folders_folderid_seq OWNED BY folders.folderid;


--
-- TOC entry 181 (class 1259 OID 19520)
-- Name: props; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
--

CREATE TABLE props (
    propid integer NOT NULL,
    key character varying(32),
    value character varying(64)
);


ALTER TABLE props OWNER TO postgres;

--
-- TOC entry 180 (class 1259 OID 19518)
-- Name: props_propid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE props_propid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE props_propid_seq OWNER TO postgres;

--
-- TOC entry 2323 (class 0 OID 0)
-- Dependencies: 180
-- Name: props_propid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE props_propid_seq OWNED BY props.propid;


--
-- TOC entry 178 (class 1259 OID 19033)
-- Name: users; Type: TABLE; Schema: public; Owner: postgres; Tablespace:
--

CREATE TABLE users (
    userid integer NOT NULL,
    boxuserid character varying(24) NOT NULL,
    name character varying(50),
    login character varying(50)
);


ALTER TABLE users OWNER TO postgres;

--
-- TOC entry 179 (class 1259 OID 19036)
-- Name: users_userid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE users_userid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE users_userid_seq OWNER TO postgres;

--
-- TOC entry 2324 (class 0 OID 0)
-- Dependencies: 179
-- Name: users_userid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE users_userid_seq OWNED BY users.userid;


--
-- TOC entry 2177 (class 2604 OID 19038)
-- Name: eventid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY events ALTER COLUMN eventid SET DEFAULT nextval('events_eventid_seq'::regclass);


--
-- TOC entry 2178 (class 2604 OID 19039)
-- Name: fileid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY files ALTER COLUMN fileid SET DEFAULT nextval('files_id_seq'::regclass);


--
-- TOC entry 2179 (class 2604 OID 19040)
-- Name: folderid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY folders ALTER COLUMN folderid SET DEFAULT nextval('folders_folderid_seq'::regclass);


--
-- TOC entry 2181 (class 2604 OID 19523)
-- Name: propid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY props ALTER COLUMN propid SET DEFAULT nextval('props_propid_seq'::regclass);


--
-- TOC entry 2180 (class 2604 OID 19041)
-- Name: userid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY users ALTER COLUMN userid SET DEFAULT nextval('users_userid_seq'::regclass);


--
-- TOC entry 2183 (class 2606 OID 19043)
-- Name: boxeventid; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY events
    ADD CONSTRAINT boxeventid UNIQUE (boxeventid);



ALTER TABLE ONLY props
    ADD CONSTRAINT propkey UNIQUE (key);



--
-- TOC entry 2198 (class 2606 OID 19045)
-- Name: boxuserid; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY users
    ADD CONSTRAINT boxuserid UNIQUE (boxuserid);


--
-- TOC entry 2185 (class 2606 OID 19047)
-- Name: events_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY events
    ADD CONSTRAINT events_pkey PRIMARY KEY (eventid);


--
-- TOC entry 2189 (class 2606 OID 19049)
-- Name: files_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY files
    ADD CONSTRAINT files_pkey PRIMARY KEY (fileid);


--
-- TOC entry 2194 (class 2606 OID 19051)
-- Name: folders_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY folders
    ADD CONSTRAINT folders_pkey PRIMARY KEY (folderid);


--
-- TOC entry 2202 (class 2606 OID 19525)
-- Name: props_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY props
    ADD CONSTRAINT props_pkey PRIMARY KEY (propid);


--
-- TOC entry 2191 (class 2606 OID 19053)
-- Name: uniquefile; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY files
    ADD CONSTRAINT uniquefile UNIQUE (boxfileid, boxfileversionid);


--
-- TOC entry 2196 (class 2606 OID 19055)
-- Name: uniquefolder; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY folders
    ADD CONSTRAINT uniquefolder UNIQUE (boxfolderid);


--
-- TOC entry 2200 (class 2606 OID 19057)
-- Name: users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (userid);


--
-- TOC entry 2187 (class 1259 OID 19058)
-- Name: filefolder; Type: INDEX; Schema: public; Owner: postgres; Tablespace:
--

CREATE INDEX filefolder ON files USING btree (boxfolderid, upper((name)::text));


--
-- TOC entry 2186 (class 1259 OID 19059)
-- Name: fileid; Type: INDEX; Schema: public; Owner: postgres; Tablespace:
--

CREATE INDEX fileid ON events USING btree (boxitemid);

--
-- TOC entry 2192 (class 1259 OID 19060)
-- Name: folderparent; Type: INDEX; Schema: public; Owner: postgres; Tablespace:
--

CREATE INDEX folderparent ON folders USING btree (boxparentfolderid, upper((foldername)::text));


--
-- TOC entry 2318 (class 0 OID 0)
-- Dependencies: 6
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


-- Completed on 2015-04-11 00:32:00 AEST

--
-- PostgreSQL database dump complete
--

