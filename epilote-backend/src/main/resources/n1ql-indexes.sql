-- N1QL indexes for E-PILOTE-CONGO
-- Run these against the Couchbase cluster to optimize query performance.
--
-- Collection: school_groups
CREATE INDEX idx_groups_type ON `school_groups`(`type`) WHERE `type` IN ['school_group', 'groupe'];

-- Collection: schools
CREATE INDEX idx_schools_type ON `schools`(`type`) WHERE `type` = 'school';
CREATE INDEX idx_schools_group ON `schools`(`type`, IFMISSINGORNULL(`groupId`, `groupeId`)) WHERE `type` = 'school';

-- Collection: users
CREATE INDEX idx_users_type ON `users`(`type`) WHERE `type` = 'user';
CREATE INDEX idx_users_email ON `users`(`type`, LOWER(TRIM(`email`))) WHERE `type` = 'user';
CREATE INDEX idx_users_group ON `users`(`type`, IFMISSINGORNULL(`groupId`, `groupeId`)) WHERE `type` = 'user';
CREATE INDEX idx_users_school ON `users`(`type`, `schoolId`) WHERE `type` = 'user';

-- Collection: modules
CREATE INDEX idx_modules_type ON `modules`(`type`) WHERE `type` = 'module';

-- Collection: profils
CREATE INDEX idx_profils_group ON `profils`(`type`, IFMISSINGORNULL(`groupId`, `groupeId`)) WHERE `type` = 'profil';

-- Collection: announcements (type = platform_announcement)
CREATE INDEX idx_announcements_type ON `announcements`(`type`, `createdAt` DESC) WHERE `type` = 'platform_announcement';

-- Collection: messages (type = platform_message)
CREATE INDEX idx_messages_type ON `messages`(`type`, `createdAt` DESC) WHERE `type` = 'platform_message';

-- Collection: audit_logs
CREATE INDEX idx_audit_type ON `audit_logs`(`type`, `timestamp` DESC) WHERE `type` = 'audit_log';
CREATE INDEX idx_audit_category ON `audit_logs`(`type`, `category`, `timestamp` DESC) WHERE `type` = 'audit_log';
CREATE INDEX idx_audit_actor ON `audit_logs`(`type`, `actorId`, `timestamp` DESC) WHERE `type` = 'audit_log';

-- Collection: invoices
CREATE INDEX idx_invoices_type ON `invoices`(`type`) WHERE `type` = 'invoice';
CREATE INDEX idx_invoices_group ON `invoices`(`type`, `groupeId`) WHERE `type` = 'invoice';

-- Collection: subscriptions
CREATE INDEX idx_subscriptions_type ON `subscriptions`(`type`) WHERE `type` = 'subscription';
CREATE INDEX idx_subscriptions_group ON `subscriptions`(`type`, `groupeId`) WHERE `type` = 'subscription';
