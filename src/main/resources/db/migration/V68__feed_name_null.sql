ALTER TABLE subscribers ALTER COLUMN feed_name DROP NOT NULL;
ALTER TABLE subscriptions ALTER COLUMN feed_name DROP NOT NULL;
