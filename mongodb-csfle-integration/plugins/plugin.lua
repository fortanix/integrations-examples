-- Name: CREATE/ROTATE A 96 BYTE SECRET
-- Version: 1.0
-- Description: ## Short Description
-- This plugin can create or rotate 96 Byte Secret in DSM.
-- ### ## Introduction
-- MongoDB Client-Side Field Level Encryption (CSFLE) to securely manage encryption keys and protect sensitive data before storing it in MongoDB.
-- CSFLE ensures that data is encrypted on the client-side before being sent to the database, so MongoDB never has access to unencrypted data. In MongoDB Enterprise, a master key is the encryption keys. It encrypts the DEK which is used to encrypt required fields in a client-side document.
-- By default, this master key is created by the libmongocryptd library as a 96-byte secret and stored in an external Key Management System (KMS). To enhance security, Fortanix DSM can generate and store the master key inside DSM itself as a 96-byte SECRET type security object, ensuring it never exists outside DSM.
--
-- ## Use cases
--
-- The plugin can be used to
--
-- - Create a 96 Byte SECRET in DSM
-- - Rotate a 96 Byte SECRET in DSM
--
-- ## Input/Output JSON object format
--
-- ### "create" method
-- This method creates a 96 byte security object of type SECRET.
--
-- ### Parameters
--
-- * `name`: Name of the security object
--
-- ### Example
--
-- Input JSON
-- ```
-- {
--    "name": "master_key",
--    "method": "create"
-- }
-- ```
--
--
-- Output JSON
-- ```
-- {
--   "name": "master_key",
--   "creator": {
--     "plugin": "4ad4da1d-29ff-4a50-a713-ae4f793d57a2"
--   },
--   "key_ops": [
--     "EXPORT",
--     "APPMANAGEABLE"
--   ],
--   "state": "Active",
--   "group_id": "7184e2bc-5061-44a5-bb1e-ad437762cc48",
--   "obj_type": "SECRET",
--   "acct_id": "ad43e085-dcc3-44c2-b088-d2f4d0409383",
--   "kid": "8eccd0c9-e4d8-48b1-b30b-dc32e4f70d28",
--   ...
-- }
-- ```
--
-- ### "rotate" method
-- This method rotates a 96 byte security object of type SECRET.
--
-- ### Parameters
--
-- * `name`: Name of the existing security object
-- * `kid`: UUID of the security object
--
-- ### Example
--
-- Input JSON with name
-- ```
-- {
--    "name": "master_key",
--    "method": "rotate"
-- }
-- ```
--
-- Input JSON with kid
-- ```
-- {
--    "kid": "8eccd0c9-e4d8-48b1-b30b-dc32e4f70d28",
--    "method": "rotate"
-- }
-- ```
--
--
-- Output JSON
-- ```
-- {
--   "name": "master_key",
--   "creator": {
--     "plugin": "4ad4da1d-29ff-4a50-a713-ae4f793d57a2"
--   },
--   "key_ops": [
--     "EXPORT",
--     "APPMANAGEABLE"
--   ],
--   "state": "Active",
--   "group_id": "7184e2bc-5061-44a5-bb1e-ad437762cc48",
--   "obj_type": "SECRET",
--   "acct_id": "ad43e085-dcc3-44c2-b088-d2f4d0409383",
--   "kid": "dd7c9f04-08f4-4a5c-be52-836d837ef78e",
--   "links": {
--     "replaced": "8eccd0c9-e4d8-48b1-b30b-dc32e4f70d28"
--   },
--   ...
-- }
-- ```


function check(input)

  local allowed_keys = { name = true, method = true, kid = true }
  for key, _ in pairs(input) do
    if key == "value" then
      return nil, Error.new { status = 400, message = "Invalid input" }
    end
    if not allowed_keys[key] then
      return nil, Error.new { status = 400, message = "Invalid input: unexpected field '" .. key .. "' provided" }
    end
  end

  if input.method == "create" then
    if not input.name then
      return nil, Error.new { status = 400, message = "Invalid input: 'create' method requires 'name' field specifying the name of the SECRET" }
    end
  elseif input.method == "rotate" then
    if not input.name and not input.kid then
      return nil, Error.new { status = 400, message = "Invalid input: 'rotate' method requires either 'name' or 'kid'" }
    end
  else
    return nil, Error.new { status = 400, message = "Invalid input: method should be either 'create' or 'rotate'" }
  end
end

function generateSecret(Sobject_name, length)
   local create_sobject, err = assert(Sobject.import { name = Sobject_name, obj_type = "SECRET", value = Blob.random(length) })
   if err ~= nil  then
      return err
   end
   return create_sobject
end

function rekey(sobject, length)
  local rekey_sobject, err = sobject:rekey { name = sobject.name, value = Blob.random(length) }
  if err ~= nil  then
      return err
  end
  return rekey_sobject
end


function run(input)
  if input.method == "create" then
    return generateSecret(input.name, 96)
  elseif input.method == "rotate" then
    if input.kid ~= nil then
      local read_sobject, err = Sobject { id = input.kid }
      if err ~= nil  then
        return err
      end
      return rekey(read_sobject, 96)
    else
      if input.name ~= nil then
        local read_sobject, err = Sobject { name = input.name }
        if err ~= nil  then
          return err
        end
        return rekey(read_sobject, 96)
      end
    end
  end
end