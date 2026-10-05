import { sqliteTable, text, integer } from 'drizzle-orm/sqlite-core';
export const accounts = sqliteTable('kl_accounts', {
 id: text('id').primaryKey(), username: text('username').notNull().unique(),
 passwordHash: text('password_hash').notNull(), created: integer('created').notNull(),
 profile: text('profile').notNull().default('{}'), revision: integer('revision').notNull().default(0), updated: integer('updated').notNull().default(0)
});
export const sessions = sqliteTable('kl_sessions', {
 hash: text('hash').primaryKey(), account: text('account').notNull().references(()=>accounts.id,{onDelete:'cascade'}), expires: integer('expires').notNull()
});
export const limits = sqliteTable('kl_limits', { key: text('key').primaryKey(), count: integer('count').notNull(), expires: integer('expires').notNull() });
