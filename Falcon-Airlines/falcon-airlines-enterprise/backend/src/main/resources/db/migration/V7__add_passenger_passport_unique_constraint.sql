-- Add unique constraint on passport_number to prevent duplicate passport numbers
-- This is a defensive measure to complement application-level validation

-- Add unique constraint if it doesn't already exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'uk_passengers_passport_number'
        AND conrelid = 'passengers'::regclass
    ) THEN
        ALTER TABLE passengers 
        ADD CONSTRAINT uk_passengers_passport_number 
        UNIQUE (passport_number);
    END IF;
END $$;
