-- TextView/EditText.setImeOptions(int): add IME_FLAG_NO_PERSONALIZED_LEARNING (0x01000000) so the
-- keyboard does not learn from or store what is typed in this app. Keeps the app's own action/flags
-- (the old script replaced the whole value, dropping actions such as IME_ACTION_SEARCH).
function before(hook, param)
    local opts = param:getArgument(0)
    if opts == nil then
        return false
    end

    local flag = 16777216
    if math.floor(opts / flag) % 2 == 1 then
        return false
    end

    local fake = opts + flag
    param:setArgument(0, fake)
    return true, tostring(opts), tostring(fake)
end
