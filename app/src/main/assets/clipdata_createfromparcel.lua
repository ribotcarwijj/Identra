function after(hook, param)
    local result = param:getResult()
    if result == null or result:getItemCount() == 0 then
        return false
    end

    local fake = result:newPlainText('Identra', 'Private')
    param:setResult(fake)
    return true
end
