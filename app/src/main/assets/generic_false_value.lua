function after(hook, param)
    local result = param:getResult()
    if not result then
        return false
    end

    param:setResult(false)
    return true, 'true', 'false'
end
