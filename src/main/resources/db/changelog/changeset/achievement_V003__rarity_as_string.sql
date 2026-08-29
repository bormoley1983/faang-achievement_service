ALTER TABLE achievement
    ALTER COLUMN rarity TYPE VARCHAR(32)
    USING CASE rarity
        WHEN 0 THEN 'COMMON'
        WHEN 1 THEN 'UNCOMMON'
        WHEN 2 THEN 'RARE'
        WHEN 3 THEN 'EPIC'
        WHEN 4 THEN 'LEGENDARY'
        ELSE NULL
    END;

ALTER TABLE achievement
    ADD CONSTRAINT achievement_rarity_check
        CHECK (rarity IN ('COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY'));
