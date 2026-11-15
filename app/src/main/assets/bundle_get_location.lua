function after(hook, param)
    local result = param:getResult()
    if param:getException() ~= nil or result == nil then
        return false
    end

    local key = param:getArgument(0)
    if key ~= 'location' then
        return false
    end

    local fake = luajava.newInstance('android.location.Location', 'privacy')
    fake:setLatitude(0)
    fake:setLongitude(0)
    param:setResult(fake)
    return true, result:toString(), fake:toString()
end
