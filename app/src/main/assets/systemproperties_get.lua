function after(hook, param)
    local result = param:getResult()
    if result == nil then
        return false
    end

    local key = param:getArgument(0)

    local h = hook:getName()
    local match = string.gmatch(h, '[^/]+')
    local func = match()
    local name = match()

    --log(key .. '=' .. result .. ' name=' .. name)

    if name == 'serial' and (key == 'ro.serialno' or key == 'ro.boot.serialno') then
        local fake = param:getSetting('value.serial')
        if fake == nil then
            fake = 'unknown'
        end
        param:setResult(fake)
        return true, result, fake
    elseif name == 'build' and string.sub(key, 1, string.len('ro.build.')) == 'ro.build.' then
        local fake
        param:setResult(fake)
        return true, result, fake
    elseif name == 'operator' and (key == 'gsm.operator.alpha' or key == 'gsm.sim.operator.alpha') then
        local fake = 'unknown'
        param:setResult(fake)
        return true, result, fake
    elseif name == 'operator' and (key == 'gsm.operator.numeric' or key == 'gsm.sim.operator.numeric') then
        local fake = '00101' -- test network
        param:setResult(fake)
        return true, result, fake
    elseif name == 'operator' and (key == 'gsm.operator.iso-country' or key == 'gsm.sim.operator.iso-country') then
        local fake = 'xx'
        param:setResult(fake)
        return true, result, fake
    elseif name == 'vendor' and string.sub(key, 1, string.len('ro.vendor.')) == 'ro.vendor.' then
        local fake
        param:setResult(fake)
        return true, result, fake
    else
        return false
    end
end
