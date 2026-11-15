function after(hook, param)
    local result = param:getResult()
    if result == nil then
        return false
    end

    local key = param:getArgument(1)
    if key == nil then
        return false
    end

    local h = hook:getName()
    local match = string.gmatch(h, '[^/]+')
    local func = match()
    local name = match()

    --log(key .. '=' .. result .. ' name=' .. name)

    if name == 'android_id' and key == 'android_id' then
        local fake = param:getSetting('value.android_id')
        if fake == nil then
            fake = '0000000000000000'
        end
        param:setResult(fake)
        return true, result, fake
    elseif name == 'bluetooth_name' and key == 'bluetooth_name' then
        local fake
        param:setResult(fake)
        return true, result, fake
    else
        return false
    end
end
