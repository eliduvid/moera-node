--
-- PostgreSQL database dump
--

-- Dumped from database version 11.7
-- Dumped by pg_dump version 11.7

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: domains; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.domains (
    name character varying(63) NOT NULL,
    node_id uuid NOT NULL
);


ALTER TABLE public.domains OWNER TO moera;

--
-- Name: entries; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.entries (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    entry_type smallint NOT NULL,
    owner_name character varying(127),
    created_at timestamp without time zone NOT NULL,
    current_revision_id uuid,
    deleted_at timestamp without time zone,
    total_revisions integer NOT NULL,
    receiver_name character varying(127),
    accepted_reactions_positive character varying(255) NOT NULL,
    accepted_reactions_negative character varying(255) NOT NULL,
    reactions_visible boolean DEFAULT true NOT NULL,
    reaction_totals_visible boolean DEFAULT true NOT NULL,
    deadline timestamp without time zone,
    draft boolean DEFAULT false NOT NULL,
    draft_revision_id uuid,
    receiver_created_at timestamp without time zone,
    current_receiver_revision_id character varying(40),
    receiver_entry_id character varying(40)
);


ALTER TABLE public.entries OWNER TO moera;

--
-- Name: entry_revision_upgrades; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.entry_revision_upgrades (
    id bigint NOT NULL,
    upgrade_type smallint NOT NULL,
    entry_revision_id uuid NOT NULL
);


ALTER TABLE public.entry_revision_upgrades OWNER TO moera;

--
-- Name: entry_revisions; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.entry_revisions (
    entry_id uuid NOT NULL,
    body_src text NOT NULL,
    body text NOT NULL,
    created_at timestamp without time zone NOT NULL,
    signature bytea,
    body_src_format smallint DEFAULT 0 NOT NULL,
    heading character varying(255) DEFAULT ''::character varying NOT NULL,
    body_preview text DEFAULT ''::text NOT NULL,
    id uuid NOT NULL,
    deleted_at timestamp without time zone,
    body_format character varying(75) DEFAULT 'message'::character varying,
    signature_version smallint DEFAULT 0 NOT NULL,
    digest bytea,
    receiver_created_at timestamp without time zone,
    receiver_deleted_at timestamp without time zone,
    receiver_revision_id character varying(40),
    receiver_body_src_hash bytea
);


ALTER TABLE public.entry_revisions OWNER TO moera;

--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: moera
--

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.hibernate_sequence OWNER TO moera;

--
-- Name: options; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.options (
    node_id uuid NOT NULL,
    name character varying(128) NOT NULL,
    value character varying(4096),
    id uuid NOT NULL
);


ALTER TABLE public.options OWNER TO moera;

--
-- Name: public_pages; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.public_pages (
    id bigint NOT NULL,
    node_id uuid NOT NULL,
    after_moment bigint NOT NULL,
    before_moment bigint NOT NULL
);


ALTER TABLE public.public_pages OWNER TO moera;

--
-- Name: reaction_totals; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.reaction_totals (
    id uuid NOT NULL,
    entry_id uuid,
    entry_revision_id uuid,
    negative boolean NOT NULL,
    emoji integer NOT NULL,
    total integer NOT NULL,
    CONSTRAINT reaction_totals_check CHECK (((entry_id IS NOT NULL) OR (entry_revision_id IS NOT NULL)))
);


ALTER TABLE public.reaction_totals OWNER TO moera;

--
-- Name: reactions; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.reactions (
    id uuid NOT NULL,
    owner_name character varying(127) NOT NULL,
    entry_revision_id uuid NOT NULL,
    negative boolean NOT NULL,
    emoji integer NOT NULL,
    created_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone,
    signature_version smallint NOT NULL,
    signature bytea,
    deleted_at timestamp without time zone,
    moment bigint NOT NULL
);


ALTER TABLE public.reactions OWNER TO moera;

--
-- Name: remote_posting_verifications; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.remote_posting_verifications (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    node_name character varying(63) NOT NULL,
    posting_id character varying(40) NOT NULL,
    revision_id character varying(40),
    status smallint NOT NULL,
    error_code character varying(63),
    error_message character varying(255),
    deadline timestamp without time zone NOT NULL,
    receiver_name character varying(63)
);


ALTER TABLE public.remote_posting_verifications OWNER TO moera;

--
-- Name: remote_reaction_verifications; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.remote_reaction_verifications (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    node_name character varying(63) NOT NULL,
    posting_id character varying(40) NOT NULL,
    reaction_owner_name character varying(63) NOT NULL,
    status smallint NOT NULL,
    error_code character varying(63),
    error_message character varying(255),
    deadline timestamp without time zone NOT NULL
);


ALTER TABLE public.remote_reaction_verifications OWNER TO moera;

--
-- Name: schema_history; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.schema_history OWNER TO moera;

--
-- Name: stories; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.stories (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    node_id uuid NOT NULL,
    feed_name character varying(63) DEFAULT 'timeline'::character varying NOT NULL,
    story_type smallint DEFAULT 0 NOT NULL,
    created_at timestamp without time zone NOT NULL,
    moment bigint NOT NULL,
    viewed boolean DEFAULT false NOT NULL,
    read boolean DEFAULT false NOT NULL,
    entry_id uuid,
    published_at timestamp without time zone DEFAULT now() NOT NULL,
    pinned boolean DEFAULT false NOT NULL,
    summary character varying(512) DEFAULT ''::character varying NOT NULL,
    tracking_id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    remote_node_name character varying(63),
    remote_entry_id character varying(40)
);


ALTER TABLE public.stories OWNER TO moera;

--
-- Name: stories_reactions; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.stories_reactions (
    story_id uuid NOT NULL,
    reaction_id uuid NOT NULL
);


ALTER TABLE public.stories_reactions OWNER TO moera;

--
-- Name: subscribers; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.subscribers (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    subscription_type smallint NOT NULL,
    feed_name character varying(63),
    entry_id uuid,
    remote_node_name character varying(63) NOT NULL,
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE public.subscribers OWNER TO moera;

--
-- Name: subscriptions; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.subscriptions (
    id uuid NOT NULL,
    node_id uuid NOT NULL,
    subscription_type smallint NOT NULL,
    feed_name character varying(63),
    remote_subscriber_id character varying(40) NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    remote_feed_name character varying(63),
    remote_entry_id character varying(40),
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE public.subscriptions OWNER TO moera;

--
-- Name: tokens; Type: TABLE; Schema: public; Owner: moera
--

CREATE TABLE public.tokens (
    token character varying(45) NOT NULL,
    name character varying(127),
    admin boolean NOT NULL,
    created_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone NOT NULL,
    node_id uuid NOT NULL
);


ALTER TABLE public.tokens OWNER TO moera;

--
-- Name: domains domains_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.domains
    ADD CONSTRAINT domains_pkey PRIMARY KEY (name);


--
-- Name: entries entries_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entries
    ADD CONSTRAINT entries_pkey PRIMARY KEY (id);


--
-- Name: entry_revision_upgrades entry_revision_upgrades_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_revision_upgrades
    ADD CONSTRAINT entry_revision_upgrades_pkey PRIMARY KEY (id);


--
-- Name: entry_revisions entry_revisions_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_revisions
    ADD CONSTRAINT entry_revisions_pkey PRIMARY KEY (id);


--
-- Name: options options_node_id_name_key; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.options
    ADD CONSTRAINT options_node_id_name_key UNIQUE (node_id, name);


--
-- Name: options options_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.options
    ADD CONSTRAINT options_pkey PRIMARY KEY (id);


--
-- Name: public_pages public_pages_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.public_pages
    ADD CONSTRAINT public_pages_pkey PRIMARY KEY (id);


--
-- Name: reaction_totals reaction_totals_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.reaction_totals
    ADD CONSTRAINT reaction_totals_pkey PRIMARY KEY (id);


--
-- Name: reactions reactions_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.reactions
    ADD CONSTRAINT reactions_pkey PRIMARY KEY (id);


--
-- Name: remote_posting_verifications remote_posting_verifications_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.remote_posting_verifications
    ADD CONSTRAINT remote_posting_verifications_pkey PRIMARY KEY (id);


--
-- Name: remote_reaction_verifications remote_reaction_verifications_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.remote_reaction_verifications
    ADD CONSTRAINT remote_reaction_verifications_pkey PRIMARY KEY (id);


--
-- Name: schema_history schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.schema_history
    ADD CONSTRAINT schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: stories stories_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.stories
    ADD CONSTRAINT stories_pkey PRIMARY KEY (id);


--
-- Name: subscribers subscribers_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.subscribers
    ADD CONSTRAINT subscribers_pkey PRIMARY KEY (id);


--
-- Name: subscriptions subscriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT subscriptions_pkey PRIMARY KEY (id);


--
-- Name: tokens tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.tokens
    ADD CONSTRAINT tokens_pkey PRIMARY KEY (token);


--
-- Name: entries_current_revision_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_current_revision_id_idx ON public.entries USING btree (current_revision_id);


--
-- Name: entries_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_deadline_idx ON public.entries USING btree (deadline);


--
-- Name: entries_draft_revision_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_draft_revision_id_idx ON public.entries USING btree (draft_revision_id);


--
-- Name: entries_entry_type_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_entry_type_idx ON public.entries USING btree (entry_type);


--
-- Name: entries_node_id_deleted_at_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_node_id_deleted_at_idx ON public.entries USING btree (node_id, deleted_at);


--
-- Name: entries_node_id_draft_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_node_id_draft_idx ON public.entries USING btree (node_id, draft);


--
-- Name: entries_node_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entries_node_id_idx ON public.entries USING btree (node_id);


--
-- Name: entries_node_id_receiver_name_receiver_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX entries_node_id_receiver_name_receiver_entry_id_idx ON public.entries USING btree (node_id, receiver_name, receiver_entry_id);


--
-- Name: entry_revision_upgrades_entry_revision_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entry_revision_upgrades_entry_revision_id_idx ON public.entry_revision_upgrades USING btree (entry_revision_id);


--
-- Name: entry_revisions_created_at_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entry_revisions_created_at_idx ON public.entry_revisions USING btree (created_at);


--
-- Name: entry_revisions_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX entry_revisions_entry_id_idx ON public.entry_revisions USING btree (entry_id);


--
-- Name: options_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX options_name_idx ON public.options USING btree (name);


--
-- Name: options_node_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX options_node_id_idx ON public.options USING btree (node_id);


--
-- Name: public_pages_node_id_after_moment_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX public_pages_node_id_after_moment_idx ON public.public_pages USING btree (node_id, after_moment);


--
-- Name: public_pages_node_id_before_moment_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX public_pages_node_id_before_moment_idx ON public.public_pages USING btree (node_id, before_moment);


--
-- Name: public_pages_node_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX public_pages_node_id_idx ON public.public_pages USING btree (node_id);


--
-- Name: reaction_totals_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reaction_totals_entry_id_idx ON public.reaction_totals USING btree (entry_id);


--
-- Name: reaction_totals_entry_revision_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reaction_totals_entry_revision_id_idx ON public.reaction_totals USING btree (entry_revision_id);


--
-- Name: reactions_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reactions_deadline_idx ON public.reactions USING btree (deadline);


--
-- Name: reactions_entry_revision_id_deleted_at_owner_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reactions_entry_revision_id_deleted_at_owner_name_idx ON public.reactions USING btree (entry_revision_id, deleted_at, owner_name);


--
-- Name: reactions_entry_revision_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reactions_entry_revision_id_idx ON public.reactions USING btree (entry_revision_id);


--
-- Name: reactions_entry_revision_id_negative_emoji_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reactions_entry_revision_id_negative_emoji_idx ON public.reactions USING btree (entry_revision_id, negative, emoji);


--
-- Name: reactions_moment_entry_revision_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX reactions_moment_entry_revision_id_idx ON public.reactions USING btree (moment, entry_revision_id);


--
-- Name: remote_posting_verifications_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX remote_posting_verifications_deadline_idx ON public.remote_posting_verifications USING btree (deadline);


--
-- Name: remote_posting_verifications_node_id_node_name_posting_id_r_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX remote_posting_verifications_node_id_node_name_posting_id_r_idx ON public.remote_posting_verifications USING btree (node_id, node_name, posting_id, revision_id);


--
-- Name: remote_reaction_verifications_deadline_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX remote_reaction_verifications_deadline_idx ON public.remote_reaction_verifications USING btree (deadline);


--
-- Name: remote_reaction_verifications_node_id_node_name_posting_id__idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX remote_reaction_verifications_node_id_node_name_posting_id__idx ON public.remote_reaction_verifications USING btree (node_id, node_name, posting_id, reaction_owner_name);


--
-- Name: schema_history_s_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX schema_history_s_idx ON public.schema_history USING btree (success);


--
-- Name: stories_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_entry_id_idx ON public.stories USING btree (entry_id);


--
-- Name: stories_node_id_feed_name_moment_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_node_id_feed_name_moment_idx ON public.stories USING btree (node_id, feed_name, moment);


--
-- Name: stories_node_id_feed_name_read_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_node_id_feed_name_read_idx ON public.stories USING btree (node_id, feed_name, read);


--
-- Name: stories_node_id_feed_name_viewed_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_node_id_feed_name_viewed_idx ON public.stories USING btree (node_id, feed_name, viewed);


--
-- Name: stories_node_id_remote_node_name_remote_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_node_id_remote_node_name_remote_entry_id_idx ON public.stories USING btree (node_id, remote_node_name, remote_entry_id);


--
-- Name: stories_reactions_reaction_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_reactions_reaction_id_idx ON public.stories_reactions USING btree (reaction_id);


--
-- Name: stories_reactions_story_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX stories_reactions_story_id_idx ON public.stories_reactions USING btree (story_id);


--
-- Name: stories_tracking_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX stories_tracking_id_idx ON public.stories USING btree (tracking_id);


--
-- Name: subscribers_entry_id_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX subscribers_entry_id_idx ON public.subscribers USING btree (entry_id);


--
-- Name: subscribers_node_id_feed_name_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE INDEX subscribers_node_id_feed_name_idx ON public.subscribers USING btree (node_id, feed_name);


--
-- Name: subscriptions_node_id_subscription_type_remote_node_name_re_idx; Type: INDEX; Schema: public; Owner: moera
--

CREATE UNIQUE INDEX subscriptions_node_id_subscription_type_remote_node_name_re_idx ON public.subscriptions USING btree (node_id, subscription_type, remote_node_name, remote_subscriber_id);


--
-- Name: entries entries_current_revision_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entries
    ADD CONSTRAINT entries_current_revision_id_fkey FOREIGN KEY (current_revision_id) REFERENCES public.entry_revisions(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: entries entries_draft_revision_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entries
    ADD CONSTRAINT entries_draft_revision_id_fkey FOREIGN KEY (draft_revision_id) REFERENCES public.entry_revisions(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: entry_revision_upgrades entry_revision_upgrades_entry_revision_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_revision_upgrades
    ADD CONSTRAINT entry_revision_upgrades_entry_revision_id_fkey FOREIGN KEY (entry_revision_id) REFERENCES public.entry_revisions(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: entry_revisions entry_revisions_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.entry_revisions
    ADD CONSTRAINT entry_revisions_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: reaction_totals reaction_totals_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.reaction_totals
    ADD CONSTRAINT reaction_totals_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: reaction_totals reaction_totals_entry_revision_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.reaction_totals
    ADD CONSTRAINT reaction_totals_entry_revision_id_fkey FOREIGN KEY (entry_revision_id) REFERENCES public.entry_revisions(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: reactions reactions_entry_revision_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.reactions
    ADD CONSTRAINT reactions_entry_revision_id_fkey FOREIGN KEY (entry_revision_id) REFERENCES public.entry_revisions(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: stories stories_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.stories
    ADD CONSTRAINT stories_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: stories_reactions stories_reactions_reaction_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.stories_reactions
    ADD CONSTRAINT stories_reactions_reaction_id_fkey FOREIGN KEY (reaction_id) REFERENCES public.reactions(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: stories_reactions stories_reactions_story_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.stories_reactions
    ADD CONSTRAINT stories_reactions_story_id_fkey FOREIGN KEY (story_id) REFERENCES public.stories(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: subscribers subscribers_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: moera
--

ALTER TABLE ONLY public.subscribers
    ADD CONSTRAINT subscribers_entry_id_fkey FOREIGN KEY (entry_id) REFERENCES public.entries(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--
