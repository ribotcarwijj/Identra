function after(hook, param)
    local configuration = param:getResult()
    configuration.mcc = 0
    configuration.mnc = 0
    return true
end